package com.xdev.osm_mobile.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.network.models.AuthResponse
import com.xdev.osm_mobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.IOException

class LoginViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginResult = MutableLiveData<Result<AuthResponse>>()
    val loginResult: LiveData<Result<AuthResponse>> = _loginResult

    fun login(username: String, password: String, sessionManager: SessionManager) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Create field map from AuthRequest
                val fields = mapOf(
                    "grant_type" to "TOKEN",
                    "username" to username,
                    "password" to password,
                    "client_id" to "osm-client",
                    "client_secret" to "X7kP9mN2vQ8rT4wY6zA1bC3dE5fG8hJ9",
                    "scope" to "read write"
                )

                val response = RetrofitClient.instance.login(
                    grantType = "TOKEN",
                    username = username,
                    password = password
                )
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        println("AuthResponse: $authResponse")
                        // Save tokens
                        sessionManager.saveAuthTokens(
                            accessToken = authResponse.accessToken,
                            refreshToken = authResponse.refreshToken
                        )

                        // Save user data
                        authResponse.osmUser?.let { user ->
                            sessionManager.saveUser(user)
                        }

                        _loginResult.value = Result.success(authResponse)
                    } else {
                        _loginResult.value = Result.failure(Exception("Empty response"))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login failed"
                    _loginResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: IOException) {
                _loginResult.value = Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                _loginResult.value = Result.failure(Exception("Unexpected error: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}