package com.ruinkogr.chatapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.data.remote.AuthService
import com.ruinkogr.chatapp.data.remote.FcmService
import com.ruinkogr.chatapp.data.remote.dto.FcmTokenRequest
import com.ruinkogr.chatapp.data.remote.dto.LoginRequest
import com.ruinkogr.chatapp.data.remote.dto.RegisterRequest
import com.ruinkogr.chatapp.data.storage.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val fcmService: FcmService,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<Unit>?>(null)
    val loginState: StateFlow<Resource<Unit>?> = _loginState

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
                    val fcmToken = FirebaseMessaging.getInstance().getToken().await() // OLD method
                    // val installationId = FirebaseInstallations.getInstance().id.await() // Modern way

                    // 4. update FCM token on app server
                    val tokenResponse = fcmService.updateFcmToken(FcmTokenRequest(fcmToken))

                    if (tokenResponse.isSuccessful) {
                        _loginState.value = Resource.Success(Unit)
                    } else {
                        _loginState.value = Resource.Error("updateFcmToken error: ${tokenResponse.errorBody()}")
                    }

                } else {
                    _loginState.value = Resource.Error("Incorrect username or password")
                }
            } catch (e: Exception) {
                _loginState.value = Resource.Error("login error: ${e.message}")
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
                    _registerState.value = Resource.Error("Firebase create UID error")
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

                    val errorMsg = response.errorBody()?.string() ?: "Register error"
                    _registerState.value = Resource.Error(errorMsg)
                }

            } catch (e: Exception) {
                _registerState.value = Resource.Error("Registration error: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                fcmService.logout()
                FirebaseAuth.getInstance().signOut()
                tokenStorage.clearSession()
                // reset state for LaunchedEffect
                _loginState.value = null
            } catch (e: Exception) {
                // log error
            }
        }
    }

    fun resetRegisterState() {
        _registerState.value = null
    }
}
