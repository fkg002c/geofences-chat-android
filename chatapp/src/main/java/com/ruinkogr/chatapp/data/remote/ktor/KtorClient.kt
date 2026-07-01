package com.ruinkogr.chatapp.data.remote.ktor

import com.ruinkogr.chatapp.data.remote.dto.RefreshTokenRequest
import com.ruinkogr.chatapp.data.remote.dto.TokenResponse
import com.ruinkogr.chatapp.data.storage.TokenStorage
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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
                            tokenStorage.clearTokens()
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
