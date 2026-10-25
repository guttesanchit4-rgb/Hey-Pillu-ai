package com.heypillu.ai

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heypillu.ai.action.DeviceActionController
import com.heypillu.ai.voice.MarathiTtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HeyPilluApp() }
    }
}

@Composable
private fun HeyPilluApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tts = remember { MarathiTtsEngine(context) }
    val actions = remember { DeviceActionController(context) }
    var input by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Backend अजून तपासलेला नाही") }
    var busy by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("assistant") }
    val messages = remember { mutableStateListOf<Pair<String, String>>().apply {
        add("Hey Pillu" to "नमस्कार! मी Hey Pillu AI. मेसेज टाइप करा किंवा मायक्रोफोन वापरा.")
    } }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) input = spoken
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) status = "मायक्रोफोन परवानगी दिलेली नाही."
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF80CBC4),
        background = Color(0xFF101116),
        surface = Color(0xFF1A1C24)
    )) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("Hey Pillu AI", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80CBC4))
                Text(status, fontSize = 12.sp, color = Color.LightGray)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "assistant", onClick = { mode = "assistant" }, label = { Text("असिस्टंट") })
                    FilterChip(selected = mode == "girlfriend", onClick = { mode = "girlfriend" }, label = { Text("गप्पा") })
                    TextButton(onClick = {
                        scope.launch {
                            status = "Backend तपासत आहे..."
                            status = checkHealth()
                        }
                    }) { Text("कनेक्शन तपासा") }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages) { (who, text) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (who == "तुम्ही") Color(0xFF25313A) else Color(0xFF20212A)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(who, color = Color(0xFF80CBC4), fontWeight = FontWeight.SemiBold)
                                Text(text, color = Color.White)
                            }
                        }
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("मराठीत लिहा किंवा बोला…") }, maxLines = 4
                    )
                    IconButton(onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "mr-IN")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "बोला, मी ऐकतेय")
                            }
                            speechLauncher.launch(intent)
                        } catch (_: Exception) { status = "या फोनवर Speech Recognition उपलब्ध नाही." }
                    }) { Icon(Icons.Default.Mic, "बोला", tint = Color(0xFF80CBC4)) }
                    IconButton(enabled = input.isNotBlank() && !busy, onClick = {
                        val query = input.trim()
                        input = ""
                        messages.add("तुम्ही" to query)
                        val local = actions.runCommand(query)
                        if (local != null) {
                            messages.add("Hey Pillu" to local)
                            tts.speak(local)
                        } else {
                            scope.launch {
                                busy = true
                                val reply = chat(query, mode)
                                messages.add("Hey Pillu" to reply)
                                tts.speak(reply)
                                status = if (reply.startsWith("कनेक्शन त्रुटी")) "Backend कनेक्शन उपलब्ध नाही" else status
                                busy = false
                            }
                        }
                    }) { Icon(Icons.Default.Send, "पाठवा", tint = Color(0xFF80CBC4)) }
                }
            }
        }
    }
}

private suspend fun checkHealth(): String = withContext(Dispatchers.IO) {
    try {
        val conn = (URL(BuildConfig.DEFAULT_BACKEND_URL.trimEnd('/') + "/health").openConnection() as HttpURLConnection)
        conn.connectTimeout = 5000; conn.readTimeout = 5000; conn.requestMethod = "GET"
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        "Backend Online: $body"
    } catch (_: Exception) { "Backend Offline — URL/Internet तपासा." }
}

private suspend fun chat(message: String, mode: String): String = withContext(Dispatchers.IO) {
    try {
        val conn = (URL(BuildConfig.DEFAULT_BACKEND_URL.trimEnd('/') + "/chat").openConnection() as HttpURLConnection)
        conn.requestMethod = "POST"
        conn.connectTimeout = 10000; conn.readTimeout = 20000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.BACKEND_TOKEN}")
        val payload = JSONObject().put("message", message).put("mode", mode).put("history", JSONArray()).toString()
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        if (code in 200..299) JSONObject(body).optString("reply", "उत्तर मिळालं नाही.")
        else "कनेक्शन त्रुटी ($code): Backend settings तपासा."
    } catch (_: Exception) { "कनेक्शन त्रुटी: Backend URL आणि इंटरनेट तपासा." }
}
