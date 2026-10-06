package com.koto.app.ui.screens.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.advancedSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "koto_advanced_settings")

enum class RetryFailedCardsPolicy {
    SOON,
    END;

    companion object {
        fun fromString(value: String?): RetryFailedCardsPolicy =
            when (value?.uppercase()) {
                "END" -> END
                else -> SOON
            }
    }
}

data class AdvancedSettingsState(
    // DISPLAY & TYPOGRAPHY
    val furiganaDisplay: Boolean = true,
    val romajiDisplay: Boolean = true,
    val japaneseFontId: String = "noto_sans_jp",
    val englishFontId: String = "inter_roboto",

    // AUDIO & SPEECH
    val textToSpeech: Boolean = true,
    val speechSpeed: Float = 1.0f,
    val isJapaneseVoiceAvailable: Boolean = true,

    // STUDY & CARD INTERACTION
    val revealFuriganaOnTap: Boolean = false,
    val kanjiLookupOnHold: Boolean = true,
    val cardFlipAnimation: Boolean = true,
    val retryFailedCards: RetryFailedCardsPolicy = RetryFailedCardsPolicy.SOON,

    // CUSTOM DECK CREATION
    val autoFillOtherSide: Boolean = true,
    val autoGenerateFurigana: Boolean = true,

    // STORAGE & DATA
    val cacheSizeBytes: Long = 0L,
)

object AdvancedSettingsKeys {
    val FURIGANA_DISPLAY = booleanPreferencesKey("furigana_display")
    val ROMAJI_DISPLAY = booleanPreferencesKey("romaji_display")
    val JAPANESE_FONT_ID = stringPreferencesKey("japanese_font_id")
    val ENGLISH_FONT_ID = stringPreferencesKey("english_font_id")

    val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    val SPEECH_SPEED = floatPreferencesKey("speech_speed")

    val REVEAL_FURIGANA_ON_TAP = booleanPreferencesKey("reveal_furigana_on_tap")
    val KANJI_LOOKUP_ON_HOLD = booleanPreferencesKey("kanji_lookup_on_hold")
    val CARD_FLIP_ANIMATION = booleanPreferencesKey("card_flip_animation")
    val RETRY_FAILED_CARDS = stringPreferencesKey("retry_failed_cards")

    val AUTO_FILL_OTHER_SIDE = booleanPreferencesKey("auto_fill_other_side")
    val AUTO_GENERATE_FURIGANA = booleanPreferencesKey("auto_generate_furigana")
}

fun Preferences.toAdvancedSettingsState(
    isJapaneseVoiceAvailable: Boolean = true,
    cacheSizeBytes: Long = 0L,
): AdvancedSettingsState {
    return AdvancedSettingsState(
        furiganaDisplay = this[AdvancedSettingsKeys.FURIGANA_DISPLAY] ?: true,
        romajiDisplay = this[AdvancedSettingsKeys.ROMAJI_DISPLAY] ?: true,
        japaneseFontId = this[AdvancedSettingsKeys.JAPANESE_FONT_ID] ?: "noto_sans_jp",
        englishFontId = this[AdvancedSettingsKeys.ENGLISH_FONT_ID] ?: "inter_roboto",
        textToSpeech = this[AdvancedSettingsKeys.TTS_ENABLED] ?: true,
        speechSpeed = this[AdvancedSettingsKeys.SPEECH_SPEED] ?: 1.0f,
        isJapaneseVoiceAvailable = isJapaneseVoiceAvailable,
        revealFuriganaOnTap = this[AdvancedSettingsKeys.REVEAL_FURIGANA_ON_TAP] ?: false,
        kanjiLookupOnHold = this[AdvancedSettingsKeys.KANJI_LOOKUP_ON_HOLD] ?: true,
        cardFlipAnimation = this[AdvancedSettingsKeys.CARD_FLIP_ANIMATION] ?: true,
        retryFailedCards = RetryFailedCardsPolicy.fromString(this[AdvancedSettingsKeys.RETRY_FAILED_CARDS]),
        autoFillOtherSide = this[AdvancedSettingsKeys.AUTO_FILL_OTHER_SIDE] ?: true,
        autoGenerateFurigana = this[AdvancedSettingsKeys.AUTO_GENERATE_FURIGANA] ?: true,
        cacheSizeBytes = cacheSizeBytes,
    )
}
