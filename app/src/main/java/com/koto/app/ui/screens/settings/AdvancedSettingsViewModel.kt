package com.koto.app.ui.screens.settings

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koto.app.feature.lesson.audio.JapaneseTtsController
import com.koto.app.feature.lesson.audio.SpeechStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class AdvancedSettingsViewModel @JvmOverloads constructor(
    application: Application,
    customDataStore: DataStore<Preferences>? = null,
) : AndroidViewModel(application) {

    private val dataStore: DataStore<Preferences> =
        customDataStore ?: application.advancedSettingsDataStore

    private val cacheSizeBytesFlow = MutableStateFlow(0L)
    private val isVoiceAvailableFlow = MutableStateFlow(true)

    val state: StateFlow<AdvancedSettingsState> = combine(
        dataStore.data,
        cacheSizeBytesFlow,
        isVoiceAvailableFlow,
    ) { preferences, cacheBytes, voiceAvailable ->
        preferences.toAdvancedSettingsState(
            isJapaneseVoiceAvailable = voiceAvailable,
            cacheSizeBytes = cacheBytes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdvancedSettingsState(
            furiganaDisplay = DisplayPreferences.get(application).displayMode.showsFurigana,
            romajiDisplay = DisplayPreferences.get(application).romajiEnabled,
            textToSpeech = JapaneseTtsController.get(application).enabled,
            speechSpeed = JapaneseTtsController.get(application).speechRate,
            englishSpeechSpeed = JapaneseTtsController.get(application).englishSpeechRate,
        ),
    )

    init {
        refreshCacheSize()
        checkVoiceAvailability()
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val totalBytes = calculateTotalCacheBytes(getApplication())
            cacheSizeBytesFlow.value = totalBytes
        }
    }

    fun checkVoiceAvailability() {
        viewModelScope.launch(Dispatchers.Default) {
            val context = getApplication<Application>()
            val ttsController = JapaneseTtsController.get(context)
            val available = ttsController.status != SpeechStatus.Unavailable
            isVoiceAvailableFlow.value = available
        }
    }

    fun setFuriganaDisplay(enabled: Boolean) {
        val app = getApplication<Application>()
        val displayPrefs = DisplayPreferences.get(app)
        val newMode = if (enabled) DisplayMode.KANJI_FURIGANA else DisplayMode.KANJI_ONLY
        displayPrefs.setDisplayMode(newMode)

        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.FURIGANA_DISPLAY] = enabled
            }
        }
    }

    fun setRomajiDisplay(enabled: Boolean) {
        val app = getApplication<Application>()
        DisplayPreferences.get(app).setRomajiEnabled(enabled)

        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.ROMAJI_DISPLAY] = enabled
            }
        }
    }

    fun setJapaneseFont(fontId: String) {
        val app = getApplication<Application>()
        DisplayPreferences.get(app).setJapaneseFont(fontId)

        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.JAPANESE_FONT_ID] = fontId
            }
        }
    }

    fun setEnglishFont(fontId: String) {
        val app = getApplication<Application>()
        DisplayPreferences.get(app).setEnglishFont(fontId)

        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.ENGLISH_FONT_ID] = fontId
            }
        }
    }

    fun setTextToSpeech(enabled: Boolean) {
        val app = getApplication<Application>()
        JapaneseTtsController.get(app).setSpeechEnabled(enabled)

        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.TTS_ENABLED] = enabled
            }
        }
    }

    fun setSpeechSpeed(speed: Float) {
        val app = getApplication<Application>()
        JapaneseTtsController.get(app).setSpeechRate(speed)

        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.SPEECH_SPEED] = speed
            }
        }
    }

    fun setEnglishSpeechSpeed(speed: Float) {
        val app = getApplication<Application>()
        JapaneseTtsController.get(app).setEnglishSpeechRate(speed)

        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.ENGLISH_SPEECH_SPEED] = speed
            }
        }
    }

    fun setRevealFuriganaOnTap(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.REVEAL_FURIGANA_ON_TAP] = enabled
            }
        }
    }

    fun setKanjiLookupOnHold(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.KANJI_LOOKUP_ON_HOLD] = enabled
            }
        }
    }

    fun setCardFlipAnimation(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.CARD_FLIP_ANIMATION] = enabled
            }
        }
    }

    fun setRetryFailedCards(policy: RetryFailedCardsPolicy) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.RETRY_FAILED_CARDS] = policy.name
            }
        }
    }

    fun setAutoFillOtherSide(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.AUTO_FILL_OTHER_SIDE] = enabled
            }
        }
    }

    fun setAutoGenerateFurigana(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AdvancedSettingsKeys.AUTO_GENERATE_FURIGANA] = enabled
            }
        }
    }

    suspend fun clearCache(): Long {
        val currentSize = cacheSizeBytesFlow.value
        withContext(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                app.cacheDir?.deleteRecursively()
                app.codeCacheDir?.deleteRecursively()
            } catch (_: Exception) {}
        }
        cacheSizeBytesFlow.value = 0L
        return currentSize
    }

    companion object {
        fun calculateTotalCacheBytes(context: Context): Long {
            var size = 0L
            try {
                val dirs = listOfNotNull(context.cacheDir, context.codeCacheDir, context.externalCacheDir)
                for (dir in dirs) {
                    if (dir.exists()) {
                        dir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                size += file.length()
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            return size
        }

        fun formatCacheSize(bytes: Long): String {
            if (bytes <= 0L) return "0.0 MB"
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f MB", mb)
        }
    }
}
