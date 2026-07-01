package com.ruinkogr.chatapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.data.remote.AuthService
import com.ruinkogr.chatapp.data.remote.dto.LoginRequest
import com.ruinkogr.chatapp.data.remote.dto.RegisterRequest
import com.ruinkogr.chatapp.data.storage.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
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
                    val tokens = response.body()!!
                    // Save tokens
                    tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
                    _loginState.value = Resource.Success(Unit)
                } else {
                    _loginState.value = Resource.Error("Wrong User name or password")
                }
            } catch (e: Exception) {
                _loginState.value = Resource.Error("Network error: ${e.localizedMessage}")
            }
        }
    }


    private val _registerState = MutableStateFlow<Resource<Unit>?>(null)
    val registerState: StateFlow<Resource<Unit>?> = _registerState

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = Resource.Loading
            try {
                val response = authService.register(RegisterRequest(username, email, password))
                if (response.isSuccessful) {
                    _registerState.value = Resource.Success(Unit)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Register error"
                    _registerState.value = Resource.Error(errorMsg)
                }
            } catch (e: Exception) {
                _registerState.value = Resource.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    fun resetRegisterState() {
        _registerState.value = null
    }
}
