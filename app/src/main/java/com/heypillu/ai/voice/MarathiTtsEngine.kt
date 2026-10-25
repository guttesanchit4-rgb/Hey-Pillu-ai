package com.heypillu.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class MarathiTtsEngine(context: Context) : TextToSpeech.OnInitListener {
    private var engine: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    @Volatile var isReady: Boolean = false
        private set

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = engine?.setLanguage(Locale("mr", "IN"))
            isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun speak(text: String, pitch: Float = 1.0f) {
        if (isReady) {
            engine?.setPitch(pitch.coerceIn(0.7f, 1.4f))
            engine?.setSpeechRate(1.0f)
            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PILLU_${System.currentTimeMillis()}")
        }
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        isReady = false
    }
}
