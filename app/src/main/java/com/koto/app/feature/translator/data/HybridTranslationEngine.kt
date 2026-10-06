package com.koto.app.feature.translator.data

import android.content.Context
import com.koto.app.BuildConfig
import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.feature.translator.model.TranslationResult
import kotlinx.coroutines.flow.StateFlow

class HybridTranslationEngine(
    private val context: Context,
    private val apiKeyProvider: () -> String = { BuildConfig.DEEPL_API_KEY },
    private val deepLApiClient: DeepLApiClient = DeepLApiClient(),
    private val mlKitEngine: TranslationEngine = MlKitTranslationEngine.getInstance(context),
    private val networkChecker: (Context) -> Boolean = { NetworkMonitor.isOnline(it) },
) : TranslationEngine {

    override val modelState: StateFlow<ModelDownloadState> = mlKitEngine.modelState

    override suspend fun checkModelStatus(): Boolean = mlKitEngine.checkModelStatus()

    override suspend fun downloadModel(): Boolean = mlKitEngine.downloadModel()

    override suspend fun translate(
        text: String,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
    ): TranslationResult {
        val clean = text.trim()
        if (clean.isBlank()) {
            return TranslationResult(
                sourceText = "",
                translatedText = "",
                romaji = "",
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                isOffline = false,
            )
        }

        if (sourceLanguage == targetLanguage) {
            val isJp = sourceLanguage == TranslationLanguage.Japanese
            val pureKana = if (isJp) KanaConverter.toPureKana(clean) else ""
            val rubyTokens = if (isJp) KanaConverter.extractRubyTokens(clean) else emptyList()
            val romaji = if (isJp) KanaConverter.toSpacedRomaji(clean) else ""
            return TranslationResult(
                sourceText = clean,
                translatedText = clean,
                kanaText = pureKana,
                rubyTokens = rubyTokens,
                romaji = romaji,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                isOffline = false,
            )
        }

        val apiKey = apiKeyProvider().trim()
        val isOnline = networkChecker(context)

        // 1. Prioritize DeepL when online and credentials exist
        if (apiKey.isNotBlank() && isOnline) {
            val deepLText = try {
                deepLApiClient.translate(clean, sourceLanguage, targetLanguage, apiKey)
            } catch (_: Throwable) {
                null
            }

            if (!deepLText.isNullOrBlank()) {
                val trimmed = deepLText.trim()
                return if (targetLanguage == TranslationLanguage.Japanese) {
                    val pureKana = KanaConverter.toPureKana(trimmed)
                    val rubyTokens = KanaConverter.extractRubyTokens(trimmed)
                    val romaji = KanaConverter.toSpacedRomaji(trimmed)
                    TranslationResult(
                        sourceText = clean,
                        translatedText = trimmed,
                        kanaText = pureKana,
                        rubyTokens = rubyTokens,
                        romaji = romaji,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        isOffline = false,
                    )
                } else {
                    val englishText = trimmed.replaceFirstChar { it.uppercase() }
                    val pureKana = KanaConverter.toPureKana(clean)
                    val rubyTokens = KanaConverter.extractRubyTokens(clean)
                    val romaji = KanaConverter.toSpacedRomaji(clean)
                    TranslationResult(
                        sourceText = clean,
                        translatedText = englishText,
                        kanaText = pureKana,
                        rubyTokens = rubyTokens,
                        romaji = romaji,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        isOffline = false,
                    )
                }
            }
        }

        // 2. Seamless offline / quota / timeout fallback to Google ML Kit
        val mlKitResult = mlKitEngine.translate(clean, sourceLanguage, targetLanguage)
        return mlKitResult.copy(isOffline = true)
    }

    companion object {
        @Volatile
        private var instance: HybridTranslationEngine? = null

        fun getInstance(context: Context): HybridTranslationEngine {
            return instance ?: synchronized(this) {
                instance ?: HybridTranslationEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
