package com.koto.app.feature.translator.model

import com.koto.app.ui.screens.settings.RubyToken

enum class TranslationLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
) {
    English("en", "English", "English"),
    Japanese("ja", "Japanese", "日本語");

    fun opposite(): TranslationLanguage = when (this) {
        English -> Japanese
        Japanese -> English
    }
}

data class SavedTranslationCard(
    val id: String,
    val sourceText: String,
    val targetText: String,
    val targetRomaji: String = "",
    val sourceLanguage: TranslationLanguage,
    val targetLanguage: TranslationLanguage,
    val timestamp: Long = System.currentTimeMillis(),
    val targetKana: String = "",
    val targetKanji: String = "",
)

data class TranslationResult(
    val sourceText: String,
    val translatedText: String,
    val romaji: String = "",
    val sourceLanguage: TranslationLanguage,
    val targetLanguage: TranslationLanguage,
    val kanaText: String = "",
    val rubyTokens: List<RubyToken> = emptyList(),
    val isOffline: Boolean = false,
)

data class TranslatorUiState(
    val sourceLanguage: TranslationLanguage = TranslationLanguage.English,
    val targetLanguage: TranslationLanguage = TranslationLanguage.Japanese,
    val inputText: String = "",
    val translatedText: String = "",
    val translatedRomaji: String = "",
    val isTranslating: Boolean = false,
    val isStarred: Boolean = false,
    val starredCards: List<SavedTranslationCard> = emptyList(),
    val showSavedCards: Boolean = false,
    val swapRotationDegrees: Float = 0f,
    val feedbackToast: String? = null,
) {
    val isActiveMode: Boolean
        get() = inputText.isNotBlank()
}
