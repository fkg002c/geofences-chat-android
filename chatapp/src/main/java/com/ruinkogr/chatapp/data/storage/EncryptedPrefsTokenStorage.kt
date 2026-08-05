@file:Suppress("DEPRECATION")

package com.ruinkogr.chatapp.data.storage

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedPrefsTokenStorage(context: Context) : TokenStorage {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_tokens_auth",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) { getAccessTokenSync() }

    override suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) { getRefreshTokenSync() }

    override suspend fun saveAccessToken(token: String) = withContext(Dispatchers.IO) { saveAccessTokenSync(token) }

    override suspend fun saveSession(userId: Int, accessToken: String, refreshToken: String) = withContext(Dispatchers.IO) { saveSessionSync(userId, accessToken, refreshToken) }

    override suspend fun clearSession() = withContext(Dispatchers.IO) { clearSessionSync() }

    override suspend fun getCurrentUserId(): Int = withContext(Dispatchers.IO) { getCurrentUserIdSync() }

    // Extra sync methods for OkHttp Authenticator
    fun getAccessTokenSync(): String? = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshTokenSync(): String? = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    fun saveAccessTokenSync(token: String) = sharedPreferences.edit(commit = true) { putString(KEY_ACCESS_TOKEN, token) }

    fun clearSessionSync() = sharedPreferences.edit(commit = true) { remove(KEY_ACCESS_TOKEN); remove(KEY_REFRESH_TOKEN); remove(KEY_USER_ID) }

    fun getCurrentUserIdSync(): Int = sharedPreferences.getInt(KEY_USER_ID, -1)
    fun saveSessionSync(id: Int, accessToken: String, refreshToken: String) {
        sharedPreferences.edit {
            putInt(KEY_USER_ID, id)
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "current_user_id" // Новый ключ
    }
}