package com.ruinkogr.chatapp.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ruinkogr.chatapp.data.local.MessageDao
import com.ruinkogr.chatapp.data.local.MessageEntity
import com.ruinkogr.chatapp.data.remote.FcmService
import com.ruinkogr.chatapp.data.remote.dto.FcmTokenRequest
import com.ruinkogr.chatapp.ui.users.ServerStatusMonitor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var fcmService: FcmService
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "onMessageReceived type: ${remoteMessage.data["type"]}, data: ${remoteMessage.data}, notif: ${remoteMessage.notification} ")

        if (remoteMessage.data["type"] == "SERVER_STATUS") {
            val status = remoteMessage.data["status"]
            val isOnline = status == "start"

            // Обновляем состояние для Compose (актуально, если приложение открыто)
            ServerStatusMonitor.setStatus(isOnline)

            // Если приложение в бэкграунде, дополнительно показываем системную нотификацию
            if (!isAppInForeground()) {
                showServerNotification(isOnline)
            }
            return
        }

        if (remoteMessage.data["type"] == "CHAT_MESSAGE" || remoteMessage.data.containsKey("id")) {
            val id = remoteMessage.data["id"]?.toIntOrNull() ?: return
            val senderId = remoteMessage.data["senderId"]?.toIntOrNull() ?: return
            val senderName = remoteMessage.data["senderName"] ?: "Unknown"
            val receiverId = remoteMessage.data["receiverId"]?.toIntOrNull() ?: return
            val content = remoteMessage.data["content"] ?: return
            val createdAt = remoteMessage.data["createdAt"] ?: return

            val incomingMessage = MessageEntity(
                id = id,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                isRead = false,
                createdAt = createdAt
            )

            // Direct DB insert
            serviceScope.launch {
                messageDao.insertMessages(listOf(incomingMessage))
                // if chat screen is open then new message appears

                if (!isAppInForeground()) {
                    showChatNotification(senderName, content)
                }
            }
        }

        if (remoteMessage.data.isNotEmpty()) {
            val senderId = remoteMessage.data["senderId"]?.toIntOrNull() ?: return
            val receiverId = remoteMessage.data["receiverId"]?.toIntOrNull() ?: return
            val content = remoteMessage.data["content"] ?: return
            val id = remoteMessage.data["id"]?.toIntOrNull() ?: return
            val createdAt = remoteMessage.data["createdAt"] ?: return

            val incomingMessage = MessageEntity(
                id = id,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                isRead = false,
                createdAt = createdAt
            )


            // TODO : Notification
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "onNewToken: $token")

        serviceScope.launch {
            try {
                fcmService.updateFcmToken(FcmTokenRequest(fcmToken = token))
            } catch (e: Exception) {
                Log.e(TAG, "updateFcmToken error: ${e.message}")
            }
        }
    }

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        Log.i(TAG, "onRegistered: $installationId")
    }

    override fun onUnregistered(installationId: String) {
        super.onUnregistered(installationId)
        Log.i(TAG, "onUnregistered: $installationId")
        // TODO inform app server
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(ActivityManager::class.java)
        val appProcesses = activityManager.runningAppProcesses ?: return false
        return appProcesses.any { it.processName == packageName && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
    }

    private fun showServerNotification(isOnline: Boolean) {
        val channelId = "server_status_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(channelId, "Статус Сервера", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val title = if (isOnline) "Сервер онлайн 🚀" else "Технические работы 🛠️"
        val body = if (isOnline) "Мы снова в сети!" else "Сервер уходит на обслуживание."

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.stat_notify_sync) // замените на свою иконку
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(999, notification)
    }

    fun showChatNotification(senderName: String, content: String) {
        val channelId = "server_status_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(channelId, "Статус Сервера", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Message from $senderName")
            .setContentText(content)
            .setUsesChronometer(true)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(999, notification)
    }


    private companion object {
        private const val TAG = "FMS"
    }
}
