package com.cookiejarapps.android.smartcookieweb.browser.home

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ChatGptActivity : AppCompatActivity() {

    private lateinit var chatHistory: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private val openAIApiKey = "sk-proj-GK0¤GK576K8V-Ax7IYKbbaNeOaZdr3yTlBeHCW1hmmREqHY6TL_StLVZEoJSfBujVqH_f979WiT3BlbkFJxFfxOW-KNoC22vS2DlXjPSxg9RCy2S6mbO9vRoSO9U7MyNsnR2AsRBB8sTc0GSTvVBXLF7wcMA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatgpt)

        chatHistory = findViewById(R.id.chat_history)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                appendMessage("Du: $msg")
                etMessage.text.clear()
                sendMessageToGPT(msg)
            }
        }
    }

    private fun appendMessage(message: String) {
        runOnUiThread {
            chatHistory.append("$message\n\n")
        }
    }

    private fun sendMessageToGPT(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val jsonBody = JSONObject()
                jsonBody.put("model", "gpt-3.5-turbo")
                val messages = org.json.JSONArray()
                messages.put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", message)
                    }
                )
                jsonBody.put("messages", messages)

                val body = jsonBody.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $openAIApiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val resStr = response.body?.string()
                if (resStr != null) {
                    val resObj = JSONObject(resStr)
                    val choices = resObj.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val reply = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        appendMessage("ChatGPT: $reply")
                    } else {
                        appendMessage("ChatGPT: [Tomt svar]")
                    }
                } else {
                    appendMessage("ChatGPT: [Inget svar från servern]")
                }
            } catch (e: Exception) {
                appendMessage("ChatGPT: [Fel: ${e.localizedMessage}]")
            }
        }
    }
}
