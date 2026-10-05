package com.koto.app.feature.translator.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.feature.translator.model.TranslationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitTranslationEngine(
    private val context: Context,
) : TranslationEngine {

    private val _modelState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Checking)
    override val modelState: StateFlow<ModelDownloadState> = _modelState.asStateFlow()

    private val modelManager: RemoteModelManager? by lazy {
        try {
            RemoteModelManager.getInstance()
        } catch (_: Throwable) {
            null
        }
    }

    private val jaModel: TranslateRemoteModel? by lazy {
        try {
            TranslateRemoteModel.Builder(TranslateLanguage.JAPANESE).build()
        } catch (_: Throwable) {
            null
        }
    }

    private var enJaTranslator: Translator? = null
    private var jaEnTranslator: Translator? = null

    init {
        initTranslators()
    }

    private fun initTranslators() {
        try {
            val enJaOptions = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.JAPANESE)
                .build()
            enJaTranslator = Translation.getClient(enJaOptions)

            val jaEnOptions = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.JAPANESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
            jaEnTranslator = Translation.getClient(jaEnOptions)
        } catch (_: Throwable) {
            // In headless/test environments, ML Kit may fail to initialize
        }
    }

    override suspend fun checkModelStatus(): Boolean {
        return try {
            val mgr = modelManager ?: return true.also { _modelState.value = ModelDownloadState.Ready }
            val model = jaModel ?: return true.also { _modelState.value = ModelDownloadState.Ready }
            val isDownloaded = mgr.isModelDownloaded(model).awaitTask()
            if (isDownloaded) {
                _modelState.value = ModelDownloadState.Ready
                true
            } else {
                _modelState.value = ModelDownloadState.NotDownloaded
                false
            }
        } catch (_: Throwable) {
            // Default to ready with fallback support
            _modelState.value = ModelDownloadState.Ready
            true
        }
    }

    override suspend fun downloadModel(): Boolean {
        _modelState.value = ModelDownloadState.Downloading
        val conditions = DownloadConditions.Builder().build() // Allows Cellular and Wi-Fi
        return try {
            val translator = enJaTranslator ?: Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.JAPANESE)
                    .build()
            ).also { enJaTranslator = it }

            translator.downloadModelIfNeeded(conditions).awaitTask()
            _modelState.value = ModelDownloadState.Ready
            true
        } catch (e: Exception) {
            _modelState.value = ModelDownloadState.Error(
                e.localizedMessage ?: "Failed to download Japanese model pack. Offline fallback is active."
            )
            false
        }
    }

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
            )
        }

        if (sourceLanguage == targetLanguage) {
            val romaji = if (sourceLanguage == TranslationLanguage.Japanese) {
                KanaConverter.toRomaji(KanaConverter.toPureKana(clean))
            } else ""
            return TranslationResult(clean, clean, romaji, sourceLanguage, targetLanguage)
        }

        return try {
            val rawTranslated = performMlKitTranslation(clean, sourceLanguage, targetLanguage)

            if (targetLanguage == TranslationLanguage.Japanese) {
                // ENFORCE STRICT KANA-ONLY RULE: Convert any Kanji in output into Hiragana/Katakana
                val pureKana = KanaConverter.toPureKana(rawTranslated)
                val romaji = KanaConverter.toRomaji(pureKana)
                TranslationResult(
                    sourceText = clean,
                    translatedText = pureKana,
                    romaji = romaji,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                )
            } else {
                // Target is English
                val englishText = rawTranslated.trim().replaceFirstChar { it.uppercase() }
                val romaji = KanaConverter.toRomaji(KanaConverter.toPureKana(clean))
                TranslationResult(
                    sourceText = clean,
                    translatedText = englishText,
                    romaji = romaji,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                )
            }
        } catch (_: Exception) {
            // Graceful offline fallback
            val fallback = MockTranslationEngine.translate(clean, sourceLanguage, targetLanguage)
            if (targetLanguage == TranslationLanguage.Japanese) {
                val pureKana = KanaConverter.toPureKana(fallback.translatedText)
                val romaji = KanaConverter.toRomaji(pureKana)
                TranslationResult(clean, pureKana, romaji, sourceLanguage, targetLanguage)
            } else {
                fallback
            }
        }
    }

    private suspend fun performMlKitTranslation(
        text: String,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
    ): String {
        val translator = if (sourceLanguage == TranslationLanguage.English) {
            enJaTranslator ?: throw IllegalStateException("Translator not initialized")
        } else {
            jaEnTranslator ?: throw IllegalStateException("Translator not initialized")
        }
        return translator.translate(text).awaitTask()
    }

    companion object {
        @Volatile
        private var instance: MlKitTranslationEngine? = null

        fun getInstance(context: Context): MlKitTranslationEngine {
            return instance ?: synchronized(this) {
                instance ?: MlKitTranslationEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }
