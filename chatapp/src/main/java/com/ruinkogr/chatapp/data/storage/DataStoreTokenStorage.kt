package com.ruinkogr.chatapp.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tokens_auth")

class DataStoreTokenStorage(private val context: Context) : TokenStorage {

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userIdKey = intPreferencesKey("current_user_id")

    override suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[accessTokenKey]
        }.first() // Take first and close the flow
    }

    override suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[refreshTokenKey]
        }.first()
    }

    override suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[accessTokenKey] = token
        }
    }

    override suspend fun saveSession(userId: Int, accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[userIdKey] = userId
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
        }
    }

    override suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun isUserLoggedInSync(): Boolean {
        return !context.dataStore.data.map { preferences ->
            preferences[accessTokenKey]
        }.first().isNullOrEmpty()
    }

    override suspend fun getCurrentUserId(): Int? {
        return context.dataStore.data.map { preferences ->
            preferences[userIdKey]
        }.first()
    }
}
