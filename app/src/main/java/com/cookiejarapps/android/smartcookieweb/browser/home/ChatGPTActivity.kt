package com.cookiejarapps.android.smartcookieweb.browser.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cookiejarapps.android.smartcookieweb.R
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ChatGPTActivity : AppCompatActivity() {
    // Din OpenAI API-nyckel (byt ut till din egna)
    private val API_KEY = "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"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatgpt)

        val promptEditText = findViewById<EditText>(R.id.gpt_prompt)
        val sendButton = findViewById<ImageButton>(R.id.gpt_send)
        val responseTextView = findViewById<TextView>(R.id.gpt_response)
        val scrollView = findViewById<ScrollView>(R.id.gpt_scroll)

        // Skicka när man klickar på skicka-knappen
        sendButton.setOnClickListener {
            sendPrompt(promptEditText, responseTextView, scrollView)
        }

        // Skicka när man trycker "Send" på tangentbordet
        promptEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                true
            } else {
                false
            }
        }

        // Scrolla ScrollView till botten när text ändras i EditText (fler rader)
        promptEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                promptEditText.post {
                    val layout = promptEditText.layout
                    if (layout != null) {
                        val scrollAmount = layout.getLineTop(promptEditText.lineCount) - promptEditText.height
                        if (scrollAmount > 0)
                            promptEditText.scrollTo(0, scrollAmount)
                        else
                            promptEditText.scrollTo(0, 0)
                    }
                }
            }
        })

    }

    private fun sendPrompt(
        promptEditText: EditText,
        responseTextView: TextView,
        scrollView: ScrollView
    ) {
        val prompt = promptEditText.text.toString().trim()
        if (prompt.isEmpty()) return

        // Dölj tangentbordet
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(promptEditText.windowToken, 0)

        // Rensa textfältet och scrolla upp
        promptEditText.setText("")
        promptEditText.scrollTo(0, 0)

        responseTextView.text = "Väntar på svar från ChatGPT..."

        thread {
            val answer = askChatGPT(prompt)
            runOnUiThread {
                responseTextView.text = answer
                scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }

    private fun askChatGPT(prompt: String): String {
        return try {
            val url = URL("https://api.openai.com/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("model", "gpt-4.1-mini") // byt till din modell
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            BufferedOutputStream(conn.outputStream).use { out ->
                out.write(jsonBody.toString().toByteArray())
                out.flush()
            }

            val response = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
            }

            val responseObject = JSONObject(response.toString())
            val choices = responseObject.getJSONArray("choices")
            if (choices.length() > 0) {
                choices.getJSONObject(0).getJSONObject("message").getString("content")
            } else {
                "Inget svar från ChatGPT..."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Fel: ${e.message}"
        }
    }
}
