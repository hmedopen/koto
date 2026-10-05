package com.koto.app.feature.translator.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.koto.app.feature.translator.model.TranslationLanguage
import java.util.Locale

class TranslatorTtsController private constructor(context: Context) {
    var isSpeaking by mutableStateOf(false)
        private set
    var isReady by mutableStateOf(false)
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var utteranceId = 0L

    init {
        try {
            engine = TextToSpeech(context.applicationContext) { status ->
                mainHandler.post {
                    if (status == TextToSpeech.SUCCESS) {
                        isReady = true
                        engine?.let(::observePlayback)
                    }
                }
            }
        } catch (_: RuntimeException) {
            isReady = false
        }
    }

    fun speak(text: String, language: TranslationLanguage) {
        if (text.isBlank()) return
        stop()
        if (!isReady) return
        val targetLocale = when (language) {
            TranslationLanguage.Japanese -> Locale.JAPANESE
            TranslationLanguage.English -> Locale.US
        }
        try {
            engine?.language = targetLocale
            val id = "trans-tts-${++utteranceId}"
            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        } catch (_: RuntimeException) {
            // Ignore runtime exceptions from detached TTS engines
        }
    }

    fun stop() {
        try {
            engine?.stop()
            isSpeaking = false
        } catch (_: RuntimeException) {
            // Ignored
        }
    }

    private fun observePlayback(tts: TextToSpeech) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { isSpeaking = true }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post { isSpeaking = false }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { isSpeaking = false }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                mainHandler.post { isSpeaking = false }
            }
        })
    }

    companion object {
        @Volatile
        private var instance: TranslatorTtsController? = null

        fun get(context: Context): TranslatorTtsController {
            return instance ?: synchronized(this) {
                instance ?: TranslatorTtsController(context).also { instance = it }
            }
        }
    }
}
