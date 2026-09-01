package com.ruinkogr.chatapp.ui.settings

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsManager(private val context: Context) {

    private val fcm = FirebaseMessaging.getInstance()

    val languageCodeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE_CODE] ?: ""
    }

    suspend fun saveLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE_CODE] = languageCode
        }
        applyLocale(languageCode)
    }

    fun applyLocale(languageCode: String) {
        if (languageCode.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        }
    }

    val trackServerStatusFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_TRACK_SERVER] ?: false
    }

    suspend fun setServerTracking(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TRACK_SERVER] = enabled
        }

        // Integrate your old logic here:
        try {
            if (enabled) {
                fcm.subscribeToTopic(topicName).await()
                Log.d(TAG, "Successfully subscribed to the topic \"$topicName\"")
            } else {
                fcm.unsubscribeFromTopic(topicName).await()
                Log.d(TAG, "Successfully unsubscribed to the topic \"$topicName\"")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error changing subscription: ${e.message}")
            context.dataStore.edit { preferences ->
                preferences[KEY_TRACK_SERVER] = !enabled
            }
        }
    }

    companion object {
        private const val TAG = "SettingsManager"
        private val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
        private val KEY_TRACK_SERVER = booleanPreferencesKey("track_server_status")
        private const val topicName = "server_status"

    }
}
