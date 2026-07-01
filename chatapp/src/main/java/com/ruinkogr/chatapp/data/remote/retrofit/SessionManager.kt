package com.ruinkogr.chatapp.data.remote.retrofit

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val _authEvents = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

    fun emitLogoutEvent() {
        _authEvents.tryEmit(AuthEvent.Logout)
    }
}

sealed interface AuthEvent {
    object Logout : AuthEvent
}
