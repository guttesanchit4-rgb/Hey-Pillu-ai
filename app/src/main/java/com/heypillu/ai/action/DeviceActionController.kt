package com.heypillu.ai.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class DeviceActionController(private val context: Context) {
    fun runCommand(raw: String): String? {
        val command = raw.lowercase()
        return when {
            "youtube" in command && ("उघड" in command || "open" in command || "शोध" in command || "search" in command) -> {
                val query = raw.replace(Regex("(?i).*youtube"), "").replace("उघड", "").replace("शोध", "").trim()
                val url = if (query.isBlank()) "https://www.youtube.com" else
                    "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
                open(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                "YouTube उघडत आहे."
            }
            "whatsapp" in command && ("उघड" in command || "open" in command) -> {
                val launch = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (launch != null) context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                else open(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.whatsapp.com/android/")))
                "WhatsApp उघडण्याचा प्रयत्न केला."
            }
            "सेटिंग" in command || "settings" in command -> {
                open(Intent(Settings.ACTION_SETTINGS)); "फोनच्या सेटिंग्ज उघडत आहे."
            }
            "wifi" in command || "वायफाय" in command -> {
                open(Intent(Settings.ACTION_WIFI_SETTINGS)); "Wi-Fi सेटिंग्ज उघडत आहे."
            }
            "bluetooth" in command || "ब्लूटूथ" in command -> {
                open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); "Bluetooth सेटिंग्ज उघडत आहे."
            }
            "तारीख" in command || "date" in command -> {
                "आजची तारीख ${java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("mr", "IN")).format(java.util.Date())} आहे."
            }
            "फोन" in command && ("लाव" in command || "call" in command) -> {
                open(Intent(Intent.ACTION_DIAL)); "डायलर उघडत आहे. कॉल करण्याआधी नंबर तपासा."
            }
            else -> null
        }
    }

    private fun open(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
