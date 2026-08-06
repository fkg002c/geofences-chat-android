package com.ruinkogr.chatapp.websocket

import android.util.Log
import com.google.gson.Gson
import com.ruinkogr.chatapp.data.local.MessageEntity
import com.ruinkogr.chatapp.data.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.time.Duration.Companion.milliseconds

class WebSocketManager(
    private val okHttpClient: OkHttpClient,
    private val tokenProvider: () -> String, // Функция для получения актуального JWT токена
    private val chatRepository: ChatRepository, // Внедряем ваш репозиторий Room
    private val gson: Gson = Gson() // Используется для дедупликации и парсинга
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isConnected = false
    private var shouldReconnect = false

    // Переменные для управления повторным подключением
    private var reconnectJob: Job? = null
    private var reconnectDelay = 2000L // Начальная задержка 2 секунды

    fun connect() {
        shouldReconnect = true // Разрешаем переподключение, так как приложение в Foreground
        if (isConnected) return // Чтобы не открывать дублирующие сокеты

        val token = tokenProvider()
        if (token.isEmpty()) return // Нет смысла подключаться без токена

        val request = Request.Builder()
            .url("wss://ruinkogr.ru") // URL вашего Node.js сервера
            .addHeader("Authorization", "Bearer $token") // Передаем токен для авторизации
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected successfully")
                isConnected = true
                // Можно запросить у Node.js пропущенные сообщения (синхронизация)
                resetReconnectDelay() // Сбрасываем таймеры при успешном подключении
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                scope.launch {
                    try {
                        // 1. Парсим строку JSON в data-класс вашего сообщения
                        val incomingMessage = gson.fromJson(text, MessageEntity::class.java)

                        // 2. Сохраняем в Room.
                        // Благодаря Flow в Room, UI автоматически отобразит новое сообщение!
                        chatRepository.saveMessage(incomingMessage)
                        // TODO: REPLACE стратегия может вызвать эффект мерцания сообщения
                        //  Предлагается проверять наличие через chatRepository.checkIfMessageExists(incomingMessage.id)
                        //  и пропускать saveMessage() и sendAck()

                        // 3. (Опционально) Отправляем ACK (подтверждение доставки на клиент)
                        sendAck(incomingMessage.id)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing or saving message: ${e.message}")
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Connection is closing, code: $code ($reason)")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Connection is closed, code: $code ($reason)")
                isConnected = false
                attemptReconnect() // Пробуем переподключиться, если закрылось неожиданно
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "Connection failure: ${t.message}")
                attemptReconnect() // Интернет пропал или сервер упал — запускаем реконнект
            }
        })
    }

    fun disconnect() {
        shouldReconnect = false // Запрещаем авто-реконнект, приложение уходит в фон
        reconnectJob?.cancel() // Отменяем запланированные попытки

        if (!isConnected) return
        webSocket?.close(1000, "App went to background")
        webSocket = null
        isConnected = false
    }

    private fun sendAck(messageId: Int) {
        val ackJson = "{\"type\":\"ACK\",\"messageId\":\"$messageId\"}"
        webSocket?.send(ackJson)
    }

    // Логика Exponential Backoff (2с -> 4с -> 8с -> 16с -> 32с -> 64с)
    private fun attemptReconnect() {
        if (!shouldReconnect) return // Если приложение в фоне, ничего не делаем

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            Log.d(TAG, "Reconnecting in ${reconnectDelay / 1000} seconds...")
            delay(reconnectDelay.milliseconds)

            // Увеличиваем задержку в 2 раза для следующего шага
            reconnectDelay = (reconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)

            connect()
        }
    }

    private fun resetReconnectDelay() {
        reconnectDelay = 2000L
        reconnectJob?.cancel()
    }

    companion object {
        private const val TAG = "WebSocketManager"
        private const val MAX_RECONNECT_DELAY = 64000L // Максимальная задержка 64 секунды

    }
}
