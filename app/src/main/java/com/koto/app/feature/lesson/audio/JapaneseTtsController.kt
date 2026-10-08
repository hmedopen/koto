package com.koto.app.feature.lesson.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.koto.app.feature.lesson.model.JapaneseText
import java.util.Locale

enum class SpeechStatus { Loading, Ready, Unavailable }

/** Application-context singleton: a single Japanese engine across lessons and rotations. */
class JapaneseTtsController private constructor(context: Context) {
    private val preferences = context.getSharedPreferences("koto_lesson_settings", Context.MODE_PRIVATE)
    var enabled by mutableStateOf(preferences.getBoolean("speech", true))
        private set
    private val _speechRate = mutableStateOf(preferences.getFloat("speech_rate", 1.0f))
    val speechRate: Float get() = _speechRate.value
    private val _englishSpeechRate = mutableStateOf(preferences.getFloat("english_speech_rate", 1.0f))
    val englishSpeechRate: Float get() = _englishSpeechRate.value
    var status by mutableStateOf(SpeechStatus.Loading)
        private set
    var isSpeaking by mutableStateOf(false)
        private set
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var utterance = 0L
    private var activeUtteranceId: String? = null
    private var unavailableNoticeShown = false

    fun takeUnavailableNotice(): Boolean {
        if (status != SpeechStatus.Unavailable || unavailableNoticeShown) return false
        unavailableNoticeShown = true
        return true
    }

    init {
        try {
            engine = TextToSpeech(context.applicationContext) { result ->
                // Post so even an immediate callback cannot race engine assignment.
                mainHandler.post {
                    val tts = engine
                    status = if (result == TextToSpeech.SUCCESS && tts != null) {
                        try {
                            val language = tts.setLanguage(Locale.JAPAN)
                            // setLanguage selects the engine's default Japanese voice. Some
                            // engines can speak Japanese without exposing an offline Voice
                            // entry, so do not reject a successful language selection.
                            if (language >= TextToSpeech.LANG_AVAILABLE) {
                                observePlayback(tts)
                                try { tts.setSpeechRate(speechRate) } catch (_: RuntimeException) {}
                                SpeechStatus.Ready
                            } else SpeechStatus.Unavailable
                        } catch (_: RuntimeException) { SpeechStatus.Unavailable }
                    } else SpeechStatus.Unavailable
                    if (status != SpeechStatus.Ready) clearPlayback()
                }
            }
        } catch (_: RuntimeException) { status = SpeechStatus.Unavailable; clearPlayback() }
    }

    fun setSpeechEnabled(value: Boolean) {
        enabled = value
        preferences.edit().putBoolean("speech", value).apply()
        if (!value) stop()
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        preferences.edit().putFloat("speech_rate", rate).apply()
        try { engine?.setSpeechRate(rate) } catch (_: RuntimeException) {}
    }

    fun setEnglishSpeechRate(rate: Float) {
        _englishSpeechRate.value = rate
        preferences.edit().putFloat("english_speech_rate", rate).apply()
        try { engine?.setSpeechRate(rate) } catch (_: RuntimeException) {}
    }

    fun speak(text: JapaneseText) {
        if (!enabled || status != SpeechStatus.Ready || !canSpeak(text)) return
        try {
            engine?.language = Locale.JAPAN
            try { engine?.setSpeechRate(speechRate) } catch (_: RuntimeException) {}
            val utteranceId = "koto-${++utterance}"
            activeUtteranceId = utteranceId
            val speechString = text.furigana?.takeIf { it.isNotBlank() } ?: text.tts
            if (engine?.speak(speechString.replace("___", "、"), TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.ERROR) {
                status = SpeechStatus.Unavailable
                clearPlayback(utteranceId)
            }
        } catch (_: RuntimeException) { status = SpeechStatus.Unavailable; clearPlayback() }
    }

    fun speakEnglish(text: String) {
        if (!enabled || status != SpeechStatus.Ready || text.isBlank()) return
        try {
            engine?.language = Locale.US
            try { engine?.setSpeechRate(englishSpeechRate) } catch (_: RuntimeException) {}
            val utteranceId = "koto-en-${++utterance}"
            activeUtteranceId = utteranceId
            if (engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.ERROR) {
                status = SpeechStatus.Unavailable
                clearPlayback(utteranceId)
            }
        } catch (_: RuntimeException) { status = SpeechStatus.Unavailable; clearPlayback() }
    }
    fun stop() {
        activeUtteranceId = null
        isSpeaking = false
        try { engine?.stop() } catch (_: RuntimeException) { /* Engine may disconnect. */ }
    }

    private fun observePlayback(tts: TextToSpeech) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = updatePlayback(utteranceId, true)
            override fun onDone(utteranceId: String?) = updatePlayback(utteranceId, false)
            @Deprecated("Deprecated by Android, still required by the listener contract")
            override fun onError(utteranceId: String?) = updatePlayback(utteranceId, false)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = updatePlayback(utteranceId, false)
        })
    }

    private fun updatePlayback(utteranceId: String?, playing: Boolean) {
        mainHandler.post {
            if (playing) {
                if (activeUtteranceId == utteranceId) isSpeaking = true
            } else if (activeUtteranceId == utteranceId) {
                activeUtteranceId = null
                isSpeaking = false
            }
        }
    }

    private fun clearPlayback(utteranceId: String? = activeUtteranceId) {
        if (utteranceId == null || activeUtteranceId == utteranceId) {
            activeUtteranceId = null
            isSpeaking = false
        }
    }

    companion object {
        @Volatile private var instance: JapaneseTtsController? = null
        fun get(context: Context): JapaneseTtsController = instance ?: synchronized(this) {
            instance ?: JapaneseTtsController(context.applicationContext).also { instance = it }
        }
        internal fun canSpeak(text: JapaneseText): Boolean {
            val tts = (text.furigana?.takeIf { it.isNotBlank() } ?: text.tts).trim()
            if (tts.isBlank()) return false
            val hasJapanese = tts.any {
                it in '\u3040'..'\u30ff' || Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN
            }
            val hasLatin = tts.any { it in 'a'..'z' || it in 'A'..'Z' }
            return hasJapanese && !hasLatin
        }
    }
}
