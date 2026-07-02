package com.ruinkogr.chatapp.data.remote.retrofit

import com.ruinkogr.chatapp.data.remote.AuthService
import com.ruinkogr.chatapp.data.remote.dto.RefreshTokenRequest
import com.ruinkogr.chatapp.data.storage.EncryptedPrefsTokenStorage
import com.ruinkogr.chatapp.data.storage.TokenStorage
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val authServiceLazy: dagger.Lazy<AuthService>,
    private val sessionManager: SessionManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/api/auth/refresh")) {
            sessionManager.emitLogoutEvent()
            return null
        }

        synchronized(this) {
            val storage = tokenStorage as? EncryptedPrefsTokenStorage

            val currentAccessToken = storage?.getAccessTokenSync() ?: return null
            val requestToken = response.request.header("Authorization")

            // refresh if it has been refreshed in another thread
            if (requestToken != "Bearer $currentAccessToken") {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // get Refresh Token
            val refreshToken = storage.getRefreshTokenSync()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.emitLogoutEvent()
                return null
            } else {
                try {
                    // Take AuthService via Lazy now
                    val authService = authServiceLazy.get()

                    // refresh token api call
                    val refreshResponse = authService
                        .refreshTokensSync(RefreshTokenRequest(refreshToken))
                        .execute()

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newTokens = refreshResponse.body()!!

                        // Save new access token
                        storage.saveAccessTokenSync(newTokens.accessToken)

                        // repeat request with new access token
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .build()
                    } else {
                        // error on refresh tocked
                        sessionManager.emitLogoutEvent()
                        return null
                    }
                } catch (e: Exception) {
                    // Network error
                    return null
                }
            }
        }
    }
}
