package com.ruinkogr.chatapp.di

import android.content.Context
import com.ruinkogr.chatapp.data.remote.AuthService
import com.ruinkogr.chatapp.data.remote.MessagesService
import com.ruinkogr.chatapp.data.remote.UsersService
import com.ruinkogr.chatapp.data.remote.retrofit.AuthInterceptor
import com.ruinkogr.chatapp.data.remote.retrofit.RetrofitClient
import com.ruinkogr.chatapp.data.remote.retrofit.TokenAuthenticator
import com.ruinkogr.chatapp.data.storage.EncryptedPrefsTokenStorage
import com.ruinkogr.chatapp.data.storage.TokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTokenStorage(@ApplicationContext context: Context): TokenStorage {
        return EncryptedPrefsTokenStorage(context)
    }

    // for OkHttp
    @Provides
    @Singleton
    fun provideAuthInterceptor(interceptor: AuthInterceptor): Interceptor = interceptor

    @Provides
    @Singleton
    fun provideTokenAuthenticator(authenticator: TokenAuthenticator): Authenticator = authenticator

    @Provides
    @Singleton
    @Named("AuthOkHttp")
    fun provideAuthOkHttpClient(): OkHttpClient {
        return RetrofitClient.createAuthOkHttpClient()
    }

    @Provides
    @Singleton
    fun provideAuthService(@Named("AuthOkHttp") okHttpClient: OkHttpClient): AuthService {
        val retrofit = RetrofitClient.createRetrofit(okHttpClient)
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    @Named("MainOkHttp")
    fun provideMainOkHttpClient(
        authInterceptor: Interceptor,
        tokenAuthenticator: Authenticator
    ): OkHttpClient {
        return RetrofitClient.createMainOkHttpClient(authInterceptor, tokenAuthenticator)
    }

    @Provides
    @Singleton
    @Named("MainRetrofit")
    fun provideMainRetrofit(@Named("MainOkHttp") okHttpClient: OkHttpClient): Retrofit {
        return RetrofitClient.createRetrofit(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideUsersService(@Named("MainRetrofit") retrofit: Retrofit): UsersService {
        return retrofit.create(UsersService::class.java)
    }

    @Provides
    @Singleton
    fun provideMessagesService(@Named("MainRetrofit") retrofit: Retrofit): MessagesService {
        return retrofit.create(MessagesService::class.java)
    }
}
