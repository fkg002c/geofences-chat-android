package com.ruinkogr.chatapp.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.ruinkogr.chatapp.R
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.data.remote.AuthService
import com.ruinkogr.chatapp.data.remote.FcmService
import com.ruinkogr.chatapp.data.remote.dto.FcmTokenRequest
import com.ruinkogr.chatapp.data.remote.dto.LoginRequest
import com.ruinkogr.chatapp.data.remote.dto.RegisterRequest
import com.ruinkogr.chatapp.data.remote.retrofit.AuthEvent
import com.ruinkogr.chatapp.data.remote.retrofit.SessionManager
import com.ruinkogr.chatapp.data.storage.TokenStorage
import com.ruinkogr.chatapp.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val fcmService: FcmService,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<Unit>?>(null)
    val loginState: StateFlow<Resource<Unit>?> = _loginState

    init {
        // A request anywhere in the app can detect an invalid session (expired refresh token, etc.)
        // and report it here so the UI always falls back to the login screen, not just on the
        // explicit Logout button.
        viewModelScope.launch {
            sessionManager.authEvents.collect { event ->
                if (event is AuthEvent.Logout) {
                    performLocalLogout()
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading
            try {
                val response = authService.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // 2. FirebaseAuth
                    val firebaseAuth = FirebaseAuth.getInstance()
                    firebaseAuth.signInWithEmailAndPassword(loginResponse.email, password).await()

                    // 5. Success -> save session with id and tokens. REQUIRED here for updateFcmToken below
                    tokenStorage.saveSession(
                        loginResponse.id,
                        loginResponse.accessToken,
                        loginResponse.refreshToken
                    )

                    // 3. get FCM Id
                    @Suppress("DEPRECATION") // TODO fix
                    val fcmToken = FirebaseMessaging.getInstance().token.await() // OLD method
                    // val installationId = FirebaseInstallations.getInstance().id.await() // Modern way

                    // 4. update FCM token on app server
                    val tokenResponse = fcmService.updateFcmToken(FcmTokenRequest(fcmToken))

                    if (tokenResponse.isSuccessful) {
                        _loginState.value = Resource.Success(Unit)
                    } else {
                        _loginState.value = Resource.Error(
                            UiText.StringResource(R.string.error_update_fcm_token_format, listOf(tokenResponse.errorBody().toString()))
                        )
                    }

                } else {
                    _loginState.value = Resource.Error(UiText.StringResource(R.string.error_incorrect_credentials))
                }
            } catch (e: Exception) {
                _loginState.value = Resource.Error(
                    UiText.StringResource(R.string.error_login_format, listOf(e.message ?: ""))
                )
            }
        }
    }


    private val _registerState = MutableStateFlow<Resource<Unit>?>(null)
    val registerState: StateFlow<Resource<Unit>?> = _registerState

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = Resource.Loading
            try {
                val firebaseAuth = FirebaseAuth.getInstance()
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUid = authResult.user?.uid

                if (firebaseUid == null) {
                    _registerState.value = Resource.Error(UiText.StringResource(R.string.error_firebase_uid))
                    return@launch
                }

                val request = RegisterRequest(
                    username = username,
                    email = email,
                    password = password,
                    firebaseUid = firebaseUid // FB UID
                )

                val response = authService.register(request)

                if (response.isSuccessful) {
                    _registerState.value = Resource.Success(Unit)
                } else {
                    // Clean FB UID
                    authResult.user?.delete()?.await()

                    val errorBody = response.errorBody()?.string()
                    val message = if (errorBody != null) {
                        UiText.DynamicString(errorBody)
                    } else {
                        UiText.StringResource(R.string.error_register_generic)
                    }
                    _registerState.value = Resource.Error(message)
                }

            } catch (e: Exception) {
                _registerState.value = Resource.Error(
                    UiText.StringResource(R.string.error_registration_format, listOf(e.localizedMessage ?: ""))
                )
            }
        }
    }

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            // Notifying the server is best-effort: a flaky connection or an expired token here
            // must never prevent the local sign-out below, or the user gets stuck on-screen.
            try {
                fcmService.logout()
            } catch (e: Exception) {
                Log.w(TAG, "Remote logout call failed, logging out locally anyway: ${e.message}")
            }
            performLocalLogout()
        }
    }

    private suspend fun performLocalLogout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth signOut failed: ${e.message}")
        }
        tokenStorage.clearSession()
        // _loginState is Activity-scoped and otherwise never clears itself: leaving a stale
        // Resource.Success here would make LoginScreen's success LaunchedEffect fire again the
        // instant it recomposes, immediately bouncing straight back past the login screen.
        _loginState.value = null
        _logoutEvent.emit(Unit)
    }

    fun resetLoginState() {
        _loginState.value = null
    }

    fun resetRegisterState() {
        _registerState.value = null
    }

    private companion object {
        private const val TAG = "AuthViewModel"
    }
}
