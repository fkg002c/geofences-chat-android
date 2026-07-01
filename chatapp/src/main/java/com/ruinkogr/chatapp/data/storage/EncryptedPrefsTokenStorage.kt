package com.ruinkogr.chatapp.data.storage

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedPrefsTokenStorage(context: Context) : TokenStorage {

    // Crypto key
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_tokens_auth",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // SharedPreferences has sync API
    override suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString("KEY_ACCESS_TOKEN", null)
    }

    override suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString("KEY_REFRESH_TOKEN", null)
    }

    override suspend fun saveAccessToken(token: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit { putString("KEY_ACCESS_TOKEN", token) }
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit {
            putString("KEY_ACCESS_TOKEN", accessToken)
                .putString("KEY_REFRESH_TOKEN", refreshToken)
        }
    }

    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        sharedPreferences.edit { clear() }
    }

    // Extra sync methods for OkHttp Authenticator
    fun getAccessTokenSync(): String? = sharedPreferences.getString("KEY_ACCESS_TOKEN", null)
    fun getRefreshTokenSync(): String? = sharedPreferences.getString("KEY_REFRESH_TOKEN", null)
    fun saveAccessTokenSync(token: String) =
        sharedPreferences.edit(commit = true) { putString("KEY_ACCESS_TOKEN", token) }

    fun clearTokensSync() = sharedPreferences.edit(commit = true) { remove("KEY_ACCESS_TOKEN"); remove("KEY_REFRESH_TOKEN") }

    override suspend fun isUserLoggedInSync(): Boolean {
        val token = sharedPreferences.getString("KEY_ACCESS_TOKEN", null)
        return !token.isNullOrEmpty()
    }
}