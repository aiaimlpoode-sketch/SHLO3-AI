package com.shlo3.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    // بعد نشر الخادم، ضع رابطه الحقيقي الذي يبدأ بـ https:// هنا.
    private val BASE_URL = "https://YOUR-SERVER-DOMAIN.example"
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView
    private var history = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(13, 20, 35)
        window.navigationBarColor = Color.rgb(13, 20, 35)
        buildUi()
        addMessage("SHLO3 AI", "هلا بيك! آني SHLO3 AI. أگدر أساعدك بالأسئلة والبرمجة وأكواد Minecraft. إذا أعطيتك كود، تگدر تنسخه من زر «نسخ الكود».")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(13, 20, 35))
            setPadding(18, 18, 18, 12)
        }
        val title = TextView(this).apply {
            text = "SHLO3 AI"
            textSize = 28f
            setTextColor(Color.rgb(56, 189, 248))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 4)
        }
        val subtitle = TextView(this).apply {
            text = "محادثة • برمجة • Minecraft • إنشاء صور"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(2, 8, 2, 8)
        }
        scroll = ScrollView(this).apply { addView(messages) }
        val entryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        input = EditText(this).apply {
            hint = "اكتب سؤالك أو طلب الكود..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setBackgroundColor(Color.rgb(31, 42, 61))
            setPadding(14, 8, 14, 8)
            minHeight = 52
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val send = Button(this).apply {
            text = "إرسال"
            setOnClickListener { sendChat() }
        }
        entryRow.addView(input)
        entryRow.addView(send)
        val imageButton = Button(this).apply {
            text = "🎨 إنشاء صورة من وصف"
            setOnClickListener { generateImage() }
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(entryRow)
        root.addView(imageButton)
        setContentView(root)
    }

    private fun addMessage(who: String, body: String) {
        runOnUiThread {
            val wrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(if (who == "أنت") Color.rgb(32, 65, 91) else Color.rgb(29, 39, 57))
                setPadding(12, 10, 12, 10)
            }
            val title = TextView(this).apply {
                text = who
                textSize = 12f
                setTextColor(if (who == "أنت") Color.rgb(125, 211, 252) else Color.rgb(56, 189, 248))
                setPadding(0, 0, 0, 5)
            }
            wrapper.addView(title)

            // يفصل مقاطع Markdown البرمجية حتى يمكن نسخ كل مقطع على حدة.
            val pieces = body.split("```")
            for (i in pieces.indices) {
                val piece = pieces[i]
                if (piece.isBlank()) continue
                if (i % 2 == 1) {
                    val lines = piece.split("\n", limit = 2)
                    val code = if (lines.size > 1 && lines[0].trim().matches(Regex("[A-Za-z0-9_+#.-]{1,30}"))) lines[1] else piece
                    val codeBox = TextView(this).apply {
                        text = code.trim('\n', ' ')
                        textSize = 13f
                        typeface = android.graphics.Typeface.MONOSPACE
                        setTextColor(Color.rgb(226, 232, 240))
                        setBackgroundColor(Color.rgb(8, 13, 23))
                        setPadding(12, 12, 12, 12)
                        textIsSelectable = true
                    }
                    wrapper.addView(codeBox)
                    val copy = Button(this).apply {
                        text = "نسخ الكود"
                        setOnClickListener {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SHLO3 AI code", code.trim()))
                            Toast.makeText(this@MainActivity, "تم نسخ الكود", Toast.LENGTH_SHORT).show()
                        }
                    }
                    wrapper.addView(copy)
                } else {
                    val textView = TextView(this).apply {
                        text = piece.trim()
                        textSize = 15f
                        setTextColor(Color.WHITE)
                        setPadding(0, 2, 0, 6)
                        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
                        textIsSelectable = true
                    }
                    wrapper.addView(textView)
                }
            }
            val params = LinearLayout.LayoutParams(-1, -2)
            params.setMargins(0, 5, 0, 5)
            messages.addView(wrapper, params)
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun sendChat() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        input.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(input.windowToken, 0)
        addMessage("أنت", prompt)
        history.put(JSONObject().put("role", "user").put("content", prompt))
        val loading = TextView(this).apply {
            text = "SHLO3 AI\nدا أجهز الجواب..."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(12, 12, 12, 12)
        }
        messages.addView(loading)
        val loadingIndex = messages.childCount - 1
        thread {
            try {
                val response = postJson("/chat", JSONObject().put("messages", history))
                val answer = response.optString("answer", "ما وصلني جواب واضح.")
                history.put(JSONObject().put("role", "assistant").put("content", answer))
                runOnUiThread {
                    if (loadingIndex >= 0 && loadingIndex < messages.childCount) messages.removeViewAt(loadingIndex)
                    addMessage("SHLO3 AI", answer)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (loadingIndex >= 0 && loadingIndex < messages.childCount) messages.removeViewAt(loadingIndex)
                    addMessage("تنبيه", "تعذر الاتصال بالخادم. تأكد من رابط الخادم وإعداد مفتاح API.\n${e.message ?: ""}")
                }
            }
        }
    }

    private fun generateImage() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) {
            addMessage("SHLO3 AI", "اكتب وصف الصورة في مربع الكتابة، وبعدين اضغط زر إنشاء صورة.")
            return
        }
        input.setText("")
        addMessage("أنت", "أنشئ صورة: $prompt")
        val loading = TextView(this).apply {
            text = "SHLO3 AI\nدا أنشئ الصورة..."
            setTextColor(Color.LTGRAY)
            setPadding(12, 12, 12, 12)
        }
        messages.addView(loading)
        val loadingIndex = messages.childCount - 1
        thread {
            try {
                val response = postJson("/image", JSONObject().put("prompt", prompt))
                val imageUrl = response.optString("imageUrl", "")
                runOnUiThread {
                    if (loadingIndex >= 0 && loadingIndex < messages.childCount) messages.removeViewAt(loadingIndex)
                    addMessage("SHLO3 AI", if (imageUrl.isNotBlank()) "تم إنشاء الصورة. رابط الصورة:\n$imageUrl" else "الخادم ما رجّع رابط صورة.")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (loadingIndex >= 0 && loadingIndex < messages.childCount) messages.removeViewAt(loadingIndex)
                    addMessage("تنبيه", "تعذر إنشاء الصورة. افحص إعدادات الخادم ومزوّد الصور.")
                }
            }
        }
    }

    private fun postJson(path: String, body: JSONObject): JSONObject {
        val url = URL(BASE_URL.trimEnd('/') + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        conn.disconnect()
        if (code !in 200..299) throw Exception("HTTP $code: $text")
        return JSONObject(text)
    }
}
