package com.koto.app.feature.lesson.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
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
    private var engine: TextToSpeech? = null
    private var utterance = 0L
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
                Handler(Looper.getMainLooper()).post {
                    val tts = engine
                    status = if (result == TextToSpeech.SUCCESS && tts != null) {
                        try {
                            val language = tts.setLanguage(Locale.JAPAN)
                            val voice = tts.voices?.filter { it.locale.language == "ja" && !it.isNetworkConnectionRequired }
                                ?.sortedBy { it.name }?.firstOrNull()
                            if (language >= TextToSpeech.LANG_AVAILABLE && voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS)
                                SpeechStatus.Ready else SpeechStatus.Unavailable
                        } catch (_: RuntimeException) { SpeechStatus.Unavailable }
                    } else SpeechStatus.Unavailable
                }
            }
        } catch (_: RuntimeException) { status = SpeechStatus.Unavailable }
    }

    fun setSpeechEnabled(value: Boolean) {
        enabled = value
        preferences.edit().putBoolean("speech", value).apply()
        if (!value) stop()
    }
    fun speak(text: JapaneseText) {
        if (!enabled || status != SpeechStatus.Ready || !canSpeak(text)) return
        try {
            if (engine?.speak(text.tts.replace("___", "、"), TextToSpeech.QUEUE_FLUSH, null, "koto-${++utterance}") == TextToSpeech.ERROR)
                status = SpeechStatus.Unavailable
        } catch (_: RuntimeException) { status = SpeechStatus.Unavailable }
    }
    fun stop() { try { engine?.stop() } catch (_: RuntimeException) { /* Engine may disconnect. */ } }

    companion object {
        @Volatile private var instance: JapaneseTtsController? = null
        fun get(context: Context): JapaneseTtsController = instance ?: synchronized(this) {
            instance ?: JapaneseTtsController(context.applicationContext).also { instance = it }
        }
        internal fun canSpeak(text: JapaneseText): Boolean = text.tts.any { it in '\u3040'..'\u30ff' } &&
            text.tts.none { it in 'a'..'z' || it in 'A'..'Z' || Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
    }
}
