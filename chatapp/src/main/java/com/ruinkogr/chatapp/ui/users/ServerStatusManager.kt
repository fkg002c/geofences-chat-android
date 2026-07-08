package com.ruinkogr.chatapp.ui.users

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.dataStore by preferencesDataStore(name = "settings")

class ServerStatusManager(private val context: Context) {

    private val fcm = FirebaseMessaging.getInstance()
    private val serverStatusKey = booleanPreferencesKey("track_server_status")
    private val topicName = "server_status"

    val isTrackingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[serverStatusKey] ?: false }

    suspend fun toggleTracking(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[serverStatusKey] = isEnabled
        }

        try {
            if (isEnabled) {
                fcm.subscribeToTopic(topicName).await()
                Log.d(TAG, "Успешно подписались на топик $topicName")
            } else {
                fcm.unsubscribeFromTopic(topicName).await()
                Log.d(TAG, "Успешно отписались от топика $topicName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка изменения подписки", e)
        }
    }

    private companion object {
        private const val TAG = "ServerStatusManager"
    }
}
