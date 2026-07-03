package com.ruinkogr.chatapp.data.storage

interface TokenStorage {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveAccessToken(token: String)
    suspend fun saveSession(userId: Int, accessToken: String, refreshToken: String)
    suspend fun clearSession()
    suspend fun isUserLoggedInSync(): Boolean
    suspend fun getCurrentUserId(): Int?
}