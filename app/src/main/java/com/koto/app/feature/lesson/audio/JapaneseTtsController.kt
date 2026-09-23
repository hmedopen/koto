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
                            val voice = tts.voices?.filter { it.locale.language == "ja" && !it.isNetworkConnectionRequired }
                                ?.sortedBy { it.name }?.firstOrNull()
                            if (language >= TextToSpeech.LANG_AVAILABLE && voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS) {
                                observePlayback(tts)
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
    fun speak(text: JapaneseText) {
        if (!enabled || status != SpeechStatus.Ready || !canSpeak(text)) return
        try {
            val utteranceId = "koto-${++utterance}"
            activeUtteranceId = utteranceId
            if (engine?.speak(text.tts.replace("___", "、"), TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.ERROR) {
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
        internal fun canSpeak(text: JapaneseText): Boolean = text.tts.any { it in '\u3040'..'\u30ff' } &&
            text.tts.none { it in 'a'..'z' || it in 'A'..'Z' || Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
    }
}
