package com.cyberscope.reports.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyberscope.reports.data.api.ApiClient
import com.cyberscope.reports.data.local.TokenManager
import com.cyberscope.reports.data.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val isAuthenticated: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val api = ApiClient.api
    private val tokenManager = TokenManager(application)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow<Boolean>(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _userId = MutableStateFlow<Long?>(null)
    val userId: StateFlow<Long?> = _userId.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        viewModelScope.launch {
            val token = tokenManager.accessToken.first()
            _isAuthenticated.value = !token.isNullOrEmpty()
            if (!token.isNullOrEmpty()) {
                _authState.value = AuthState.Success(true)
                ApiClient.setAuthToken(token!!)
            }
        }
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val request = LoginRequest(email, password)
                val response = api.login(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val jwtResponse = response.body()!!
                    
                    // Save tokens
                    tokenManager.saveTokens(
                        accessToken = jwtResponse.accessToken,
                        refreshToken = jwtResponse.refreshToken,
                        email = jwtResponse.email,
                        name = jwtResponse.fullName,
                        role = jwtResponse.role
                    )
                    
                    // Update API client with token
                    jwtResponse.accessToken?.let { token ->
                        ApiClient.setAuthToken(token)
                    }
                    
                    // Save userId
                    jwtResponse.userId?.let { userId ->
                        _userId.value = userId
                    }
                    
                    _isAuthenticated.value = true
                    _authState.value = AuthState.Success(true)
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Login failed"
                    _authState.value = AuthState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearTokens()
            ApiClient.clearAuthToken()
            _isAuthenticated.value = false
            _authState.value = AuthState.Success(false)
        }
    }
}

