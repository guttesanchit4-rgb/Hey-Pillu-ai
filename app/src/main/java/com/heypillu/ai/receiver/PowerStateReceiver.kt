package com.heypillu.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import java.util.Locale

class PowerStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> "सर, चार्जिंग लावली आहे."
            Intent.ACTION_POWER_DISCONNECTED -> "सर, चार्जिंग काढली आहे."
            else -> return
        }
        // Receiver lifetime is short; initialize TTS and release it after speech finishes.
        val pending = goAsync()
        lateinit var tts: TextToSpeech\n        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("mr", "IN"))
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setSpeechRate(1.0f)
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "PILLU_POWER")
                }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                tts.shutdown()
                pending.finish()
            }, 2500)
        }
    }

    companion object { private const val TAG = "PowerStateReceiver" }
}
