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
    private val authService: AuthService,
    private val sessionManager: SessionManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/api/auth/refresh")) {
            return null
        }

        val storage = tokenStorage as? EncryptedPrefsTokenStorage
        val refreshToken = storage?.getRefreshTokenSync() ?: return null

        val refreshResponse = authService.refreshTokensSync(RefreshTokenRequest(refreshToken)).execute()

        return if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
            val newAccessToken = refreshResponse.body()!!.accessToken
            storage.saveAccessTokenSync(newAccessToken)

            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        } else {
            storage.clearTokensSync()

            sessionManager.emitLogoutEvent()

            null
        }
    }
}
