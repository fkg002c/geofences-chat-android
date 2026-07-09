package com.ruinkogr.chatapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import androidx.core.net.toUri
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(applicationContext)
    }

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val chatSoundUri = "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/message".toUri()
        val serverSoundUri = "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/connect".toUri()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val chatChannel = NotificationChannel(
            "chat_messages_channel",
            "Chat messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications about new messages"
            setSound(chatSoundUri, audioAttributes)
            enableVibration(true)
        }

        val serverChannel = NotificationChannel(
            "server_status_channel",
            "Server status",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications about technical work and server restarts"
            setSound(serverSoundUri, audioAttributes)
        }

        notificationManager.createNotificationChannel(chatChannel)
        notificationManager.createNotificationChannel(serverChannel)
    }
}