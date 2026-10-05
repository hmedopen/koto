package com.koto.app.feature.translator.data

import com.koto.app.feature.translator.model.TranslationLanguage
import com.koto.app.feature.translator.model.TranslationResult
import kotlinx.coroutines.flow.StateFlow

sealed class ModelDownloadState {
    data object Checking : ModelDownloadState()
    data object NotDownloaded : ModelDownloadState()
    data object Downloading : ModelDownloadState()
    data object Ready : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

interface TranslationEngine {
    val modelState: StateFlow<ModelDownloadState>

    suspend fun checkModelStatus(): Boolean
    suspend fun downloadModel(): Boolean
    suspend fun translate(
        text: String,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
    ): TranslationResult
}
