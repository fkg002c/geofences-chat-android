package com.ruinkogr.chatapp.data.storage

interface TokenStorage {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveAccessToken(token: String)
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clearTokens()
    suspend fun isUserLoggedInSync(): Boolean
}