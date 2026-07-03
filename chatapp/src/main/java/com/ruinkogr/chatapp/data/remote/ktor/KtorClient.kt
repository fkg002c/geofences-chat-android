package com.ruinkogr.chatapp.data.remote.ktor

import com.ruinkogr.chatapp.data.remote.dto.RefreshTokenRequest
import com.ruinkogr.chatapp.data.remote.dto.TokenResponse
import com.ruinkogr.chatapp.data.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun provideKtorClient(tokenStorage: TokenStorage): HttpClient {
    return HttpClient(OkHttp) {

        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(
                        accessToken = tokenStorage.getAccessToken() ?: "",
                        refreshToken = tokenStorage.getRefreshToken() ?: ""
                    )
                }

                refreshTokens {
                    val oldRefreshToken = tokenStorage.getRefreshToken() ?: return@refreshTokens null

                    try {
                        val response = client.post("https://ruinkogr.ru") {
                            contentType(ContentType.Application.Json)
                            setBody(RefreshTokenRequest(oldRefreshToken))
                            markAsRefreshTokenRequest()
                        }

                        if (response.status == HttpStatusCode.OK) {
                            val newTokens = response.body<TokenResponse>()

                            tokenStorage.saveAccessToken(newTokens.accessToken)

                            BearerTokens(
                                accessToken = newTokens.accessToken,
                                refreshToken = oldRefreshToken
                            )
                        } else {
                            tokenStorage.clearSession()
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                sendWithoutRequest { request ->
                    !request.url.encodedPath.contains("/api/auth/")
                }
            }
        }
    }
}
