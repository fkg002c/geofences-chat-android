package com.ruinkogr.chatapp.data.remote.retrofit

import com.ruinkogr.chatapp.data.remote.AuthService
import com.ruinkogr.chatapp.data.remote.dto.RefreshTokenRequest
import com.ruinkogr.chatapp.data.storage.EncryptedPrefsTokenStorage
import com.ruinkogr.chatapp.data.storage.TokenStorage
import dagger.Lazy
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authServiceLazy: Lazy<AuthService>,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Refresh request as is
        if (originalRequest.url.encodedPath.contains("/api/auth/refresh")) {
            return chain.proceed(originalRequest)
        }

        // Access Token to regular request
        val storage = tokenStorage as? EncryptedPrefsTokenStorage
        val accessToken = storage?.getAccessTokenSync()

        val requestBuilder = originalRequest.newBuilder()
        if (!accessToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        // Run request
        val initialResponse = chain.proceed(requestBuilder.build())

        if (initialResponse.code == 403 || initialResponse.code == 401) {

            // memory lear prevention
            initialResponse.close()

            synchronized(this) {
                val currentAccessToken = storage?.getAccessTokenSync()

                // check new token saved in another thread
                if (accessToken != currentAccessToken) {
                    return@synchronized executeRetryRequest(chain, originalRequest, currentAccessToken)
                }

                val refreshToken = storage?.getRefreshTokenSync()
                if (refreshToken.isNullOrBlank()) {
                    sessionManager.emitLogoutEvent()
                    return initialResponse
                }

                try {
                    val refreshResponse = authServiceLazy.get()
                        .refreshTokensSync(RefreshTokenRequest(refreshToken))
                        .execute()

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newTokens = refreshResponse.body()!!

                        storage.saveAccessTokenSync(newTokens.accessToken)

                        return executeRetryRequest(chain, originalRequest, newTokens.accessToken)
                    } else {
                        sessionManager.emitLogoutEvent()
                    }
                } catch (e: Exception) {
                    return initialResponse
                }
            }
        }

        return initialResponse
    }

    private fun executeRetryRequest(
        chain: Interceptor.Chain,
        originalRequest: Request,
        newAccessToken: String?
    ): Response {
        val retryBuilder = originalRequest.newBuilder()
        if (!newAccessToken.isNullOrBlank()) {
            retryBuilder.header("Authorization", "Bearer $newAccessToken")
        }
        return chain.proceed(retryBuilder.build())
    }
}