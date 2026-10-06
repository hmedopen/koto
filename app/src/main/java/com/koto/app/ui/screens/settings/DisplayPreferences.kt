package com.koto.app.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

enum class DisplayMode {
    KANA,
    KANJI_FURIGANA,
    KANJI_ONLY;

    val isKanji: Boolean get() = this == KANJI_FURIGANA || this == KANJI_ONLY
    val showsFurigana: Boolean get() = this == KANJI_FURIGANA
}

data class RubyToken(
    val surface: String,
    val reading: String? = null,
) {
    val hasRuby: Boolean get() = reading != null && reading != surface
}

val LocalJapaneseDisplayMode = compositionLocalOf { DisplayMode.KANA }
val LocalRomajiVisibility = compositionLocalOf { true }

/**
 * Persists Japanese display preference (Kana vs Kanji with Furigana vs Kanji Only) and Romaji guide toggle.
 * Uses Compose State for instant reactive recomposition across the app.
 */
class DisplayPreferences private constructor(context: Context) {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val _displayMode = mutableStateOf(loadDisplayMode())
    val displayMode: DisplayMode get() = _displayMode.value

    private val _romajiEnabled = mutableStateOf(preferences.getBoolean(KEY_ROMAJI, true))
    val romajiEnabled: Boolean get() = _romajiEnabled.value

    private fun loadDisplayMode(): DisplayMode {
        val raw = preferences.getString(KEY_DISPLAY_MODE, DisplayMode.KANA.name) ?: DisplayMode.KANA.name
        return when (raw) {
            "KANJI" -> DisplayMode.KANJI_FURIGANA
            else -> runCatching { DisplayMode.valueOf(raw) }.getOrDefault(DisplayMode.KANA)
        }
    }

    fun setDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
        preferences.edit().putString(KEY_DISPLAY_MODE, mode.name).apply()
    }

    fun setRomajiEnabled(enabled: Boolean) {
        _romajiEnabled.value = enabled
        preferences.edit().putBoolean(KEY_ROMAJI, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "koto_display_preferences"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_ROMAJI = "romaji_enabled"

        @Volatile
        private var instance: DisplayPreferences? = null

        fun get(context: Context): DisplayPreferences =
            instance ?: synchronized(this) {
                instance ?: DisplayPreferences(context.applicationContext).also { instance = it }
            }
    }
}
