package com.koto.app.feature.translator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.koto.app.feature.translator.data.DeepLApiClient
import com.koto.app.feature.translator.data.HybridTranslationEngine
import com.koto.app.feature.translator.data.MockTranslationEngine
import com.koto.app.feature.translator.data.ModelDownloadState
import com.koto.app.feature.translator.data.TranslationEngine
import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.feature.translator.model.TranslationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HybridTranslationEngineTest {

    private lateinit var context: Context

    private class FakeMlKitEngine : TranslationEngine {
        override val modelState: StateFlow<ModelDownloadState> =
            MutableStateFlow(ModelDownloadState.Ready)

        override suspend fun checkModelStatus(): Boolean = true
        override suspend fun downloadModel(): Boolean = true

        override suspend fun translate(
            text: String,
            sourceLanguage: TranslationLanguage,
            targetLanguage: TranslationLanguage,
        ): TranslationResult {
            val res = MockTranslationEngine.translate(text, sourceLanguage, targetLanguage)
            return res.copy(isOffline = true)
        }
    }

    private class FakeDeepLClient(
        var responseToReturn: String? = "こんにちは、世界！",
        var shouldThrow: Boolean = false,
    ) : DeepLApiClient() {
        var lastRequestedText: String? = null
        var lastSourceLang: TranslationLanguage? = null
        var lastTargetLang: TranslationLanguage? = null

        override suspend fun translate(
            text: String,
            sourceLanguage: TranslationLanguage,
            targetLanguage: TranslationLanguage,
            apiKey: String,
        ): String? {
            lastRequestedText = text
            lastSourceLang = sourceLanguage
            lastTargetLang = targetLanguage
            if (shouldThrow) {
                throw RuntimeException("Simulated HTTP 456 Quota Exceeded or Network Timeout")
            }
            return responseToReturn
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun onlineWithValidKeyRoutesToDeepLAndMarksNotOffline() = runBlocking {
        val fakeDeepL = FakeDeepLClient(responseToReturn = "こんにちは、世界！")
        val fakeMlKit = FakeMlKitEngine()

        val engine = HybridTranslationEngine(
            context = context,
            apiKeyProvider = { "valid-deepl-key:fx" },
            deepLApiClient = fakeDeepL,
            mlKitEngine = fakeMlKit,
            networkChecker = { true },
        )

        val result = engine.translate(
            text = "Hello, world!",
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.Japanese,
        )

        assertEquals("Hello, world!", result.sourceText)
        assertEquals("こんにちは、世界！", result.translatedText)
        assertFalse("Online DeepL translation should have isOffline == false", result.isOffline)
        assertEquals("Hello, world!", fakeDeepL.lastRequestedText)
    }

    @Test
    fun offlineFallsBackSilentlyToMlKitAndMarksOffline() = runBlocking {
        val fakeDeepL = FakeDeepLClient(responseToReturn = "Online Translation")
        val fakeMlKit = FakeMlKitEngine()

        val engine = HybridTranslationEngine(
            context = context,
            apiKeyProvider = { "valid-deepl-key:fx" },
            deepLApiClient = fakeDeepL,
            mlKitEngine = fakeMlKit,
            networkChecker = { false }, // Offline
        )

        val result = engine.translate(
            text = "hello",
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.Japanese,
        )

        assertEquals("こんにちは", result.translatedText)
        assertTrue("Offline translation must have isOffline == true", result.isOffline)
    }

    @Test
    fun deeplQuotaExceededOrTimeoutFallsBackSilentlyToMlKit() = runBlocking {
        val fakeDeepL = FakeDeepLClient(shouldThrow = true) // Simulates HTTP 456 Quota Exceeded
        val fakeMlKit = FakeMlKitEngine()

        val engine = HybridTranslationEngine(
            context = context,
            apiKeyProvider = { "valid-deepl-key:fx" },
            deepLApiClient = fakeDeepL,
            mlKitEngine = fakeMlKit,
            networkChecker = { true },
        )

        val result = engine.translate(
            text = "hello",
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.Japanese,
        )

        // Result should seamlessly come from ML Kit fallback without error
        assertEquals("こんにちは", result.translatedText)
        assertTrue("Fallback to ML Kit must set isOffline == true", result.isOffline)
    }

    @Test
    fun missingApiKeyFallsBackSilentlyToMlKit() = runBlocking {
        val fakeDeepL = FakeDeepLClient(responseToReturn = "Should not be called")
        val fakeMlKit = FakeMlKitEngine()

        val engine = HybridTranslationEngine(
            context = context,
            apiKeyProvider = { "" }, // Empty key
            deepLApiClient = fakeDeepL,
            mlKitEngine = fakeMlKit,
            networkChecker = { true },
        )

        val result = engine.translate(
            text = "hello",
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.Japanese,
        )

        assertEquals("こんにちは", result.translatedText)
        assertTrue(result.isOffline)
    }

    @Test
    fun blankInputReturnsImmediateEmptyResult() = runBlocking {
        val engine = HybridTranslationEngine(
            context = context,
            apiKeyProvider = { "key" },
            mlKitEngine = FakeMlKitEngine(),
            networkChecker = { true },
        )

        val result = engine.translate(
            text = "   ",
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.Japanese,
        )

        assertTrue(result.translatedText.isEmpty())
        assertFalse(result.isOffline)
    }

    @Test
    fun sameSourceAndTargetLanguageEchoesText() = runBlocking {
        val engine = HybridTranslationEngine(
            context = context,
            apiKeyProvider = { "key" },
            mlKitEngine = FakeMlKitEngine(),
            networkChecker = { true },
        )

        val result = engine.translate(
            text = "English test",
            sourceLanguage = TranslationLanguage.English,
            targetLanguage = TranslationLanguage.English,
        )

        assertEquals("English test", result.translatedText)
        assertFalse(result.isOffline)
    }
}
