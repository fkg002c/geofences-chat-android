package com.ruinkogr.chatapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import androidx.core.net.toUri
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ruinkogr.chatapp.R
import com.ruinkogr.chatapp.ui.settings.SettingsManager
import com.ruinkogr.chatapp.websocket.AppLifecycleObserver
import com.ruinkogr.chatapp.websocket.WebSocketManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ChatApplication : Application() {
    @Inject
    lateinit var webSocketManager: WebSocketManager

    override fun onCreate() {
        super.onCreate()
        SettingsManager(applicationContext).let { manager ->
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                val savedLanguage = manager.languageCodeFlow.first()
                manager.applyLocale(savedLanguage)
            }
        }
        createNotificationChannels(applicationContext)

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            AppLifecycleObserver(webSocketManager)
        )
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
            context.getString(R.string.channel_chat_messages_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_chat_messages_description)
            setSound(chatSoundUri, audioAttributes)
            enableVibration(true)
        }

        val serverChannel = NotificationChannel(
            "server_status_channel",
            context.getString(R.string.channel_server_status_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_server_status_description)
            setSound(serverSoundUri, audioAttributes)
        }

        notificationManager.createNotificationChannel(chatChannel)
        notificationManager.createNotificationChannel(serverChannel)
    }
}