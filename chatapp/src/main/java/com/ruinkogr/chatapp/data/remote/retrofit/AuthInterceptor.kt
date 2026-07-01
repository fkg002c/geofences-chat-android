package com.ruinkogr.chatapp.data.remote.retrofit

import com.ruinkogr.chatapp.data.storage.EncryptedPrefsTokenStorage
import com.ruinkogr.chatapp.data.storage.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val storage = tokenStorage as? EncryptedPrefsTokenStorage
        val accessToken = storage?.getAccessTokenSync()

        val requestBuilder = originalRequest.newBuilder()
        if (!accessToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}