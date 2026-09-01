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
    private val tokenProvider: () -> String, // Function to get the current JWT token
    private val chatRepository: ChatRepository, // Inject your Room repository
    private val gson: Gson = Gson() // Used for deduplication and parsing
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isConnected = false
    private var shouldReconnect = false

    // Variables for managing reconnection
    private var reconnectJob: Job? = null
    private var reconnectDelay = 2000L // Initial delay of 2 seconds

    fun connect() {
        shouldReconnect = true // Allow reconnecting since the app is in the foreground
        if (isConnected) return // Avoid opening duplicate sockets

        val token = tokenProvider()
        if (token.isEmpty()) return // No point connecting without a token

        val request = Request.Builder()
            .url("wss://ruinkogr.ru") // Your Node.js server URL
            .addHeader("Authorization", "Bearer $token") // Pass the token for authorization
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected successfully")
                isConnected = true
                // Could request missed messages from Node.js here (sync)
                resetReconnectDelay() // Reset timers on a successful connection
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                scope.launch {
                    try {
                        // 1. Parse the JSON string into your message data class
                        val incomingMessage = gson.fromJson(text, MessageEntity::class.java)

                        // 2. Save to Room.
                        // Thanks to Room's Flow, the UI will automatically display the new message!
                        chatRepository.saveMessage(incomingMessage)
                        // TODO: REPLACE strategy may cause a message flicker effect
                        //  Consider checking existence via chatRepository.checkIfMessageExists(incomingMessage.id)
                        //  and skipping saveMessage() and sendAck()

                        // 3. (Optional) Send ACK (delivery confirmation to the client)
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
                attemptReconnect() // Try to reconnect if it closed unexpectedly
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "Connection failure: ${t.message}")
                attemptReconnect() // Internet dropped or server went down — start reconnecting
            }
        })
    }

    fun disconnect() {
        shouldReconnect = false // Disable auto-reconnect, app is going to background
        reconnectJob?.cancel() // Cancel any scheduled reconnect attempts

        if (!isConnected) return
        webSocket?.close(1000, "App went to background")
        webSocket = null
        isConnected = false
    }

    private fun sendAck(messageId: Int) {
        val ackJson = "{\"type\":\"ACK\",\"messageId\":\"$messageId\"}"
        webSocket?.send(ackJson)
    }

    // Exponential Backoff logic (2s -> 4s -> 8s -> 16s -> 32s -> 64s)
    private fun attemptReconnect() {
        if (!shouldReconnect) return // Do nothing if the app is in the background

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            Log.d(TAG, "Reconnecting in ${reconnectDelay / 1000} seconds...")
            delay(reconnectDelay.milliseconds)

            // Double the delay for the next step
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
        private const val MAX_RECONNECT_DELAY = 64000L // Maximum delay of 64 seconds

    }
}
