package com.example.assemblymonitoring

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import android.util.Log


class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var editText: EditText
    private lateinit var button: Button
    private lateinit var client: OkHttpClient
    private lateinit var webSocket: WebSocket
    private var isWebSocketOpen = false  // Флаг для проверки, открыт ли WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        editText = findViewById(R.id.editText)
        button = findViewById(R.id.button)

        client = OkHttpClient()

        // Подключение к серверу WebSocket
        val request = Request.Builder()
            .url("ws://192.168.0.23:8080") // Адрес сервера WebSocket
            .build()

        // Создание WebSocket
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                isWebSocketOpen = true
                runOnUiThread {
                    Log.d("WebSocket", "Connected")
                    Toast.makeText(this@MainActivity, "Соединение установлено", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onMessage(webSocket: WebSocket, message: String) {
                runOnUiThread {
                    when {
                        message.contains("Печатает текст") -> {
                            textView.text = "Печатает текст..."
                            Log.d("WebSocket", "Печатает текст")
                        }
                        message.contains("Нажата кнопка!") -> {
                            textView.text = "Кнопка нажата"
                            Log.d("WebSocket", "Кнопка нажата")
                        }
                        message.contains("Вы прекратили печатать") -> {
                            textView.text = "Нет действий"
                            Toast.makeText(this@MainActivity, "Сервер уведомил о бездействии", Toast.LENGTH_SHORT).show()
                            Log.d("WebSocket", "Сообщение о бездействии")
                        }
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isWebSocketOpen = false
                webSocket.close(1000, null)
                runOnUiThread {
                    Log.d("WebSocket", "Closing: $reason")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                isWebSocketOpen = false
                runOnUiThread {
                    Log.e("WebSocket", "Error: ${t.message}")
                    Toast.makeText(this@MainActivity, "Ошибка подключения: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        })

        // Добавление слушателя для поля EditText
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Отправка сообщения при изменении текста, только если WebSocket открыт
                if (isWebSocketOpen) {
                    val message = "Печатает текст: $charSequence"
                    webSocket.send(message)
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        button.setOnClickListener {
            if (isWebSocketOpen) {
                val message = "Нажата кнопка!"
                webSocket.send(message)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Закрытие WebSocket при уничтожении активности
        if (isWebSocketOpen) {
            webSocket.close(1000, "Closing connection")
        }
    }
}

