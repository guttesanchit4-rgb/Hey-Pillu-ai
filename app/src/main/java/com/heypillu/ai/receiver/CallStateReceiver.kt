package com.heypillu.ai.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import java.util.Locale

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) != TelephonyManager.EXTRA_STATE_RINGING) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val name = if (number != null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val uri = android.net.Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number))
                context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            } catch (_: Exception) { null }
        } else null
        val announcement = if (!name.isNullOrBlank()) "सर, $name यांचा फोन आला आहे." else "सर, एका अनोळखी नंबरवरून फोन आला आहे."
        val pending = goAsync()
        lateinit var tts: TextToSpeech\n        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("mr", "IN"))
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "PILLU_CALL")
                }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                tts.shutdown()
                pending.finish()
            }, 3000)
        }
    }
}
