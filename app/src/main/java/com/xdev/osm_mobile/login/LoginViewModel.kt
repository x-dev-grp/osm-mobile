package com.xdev.osm_mobile.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.models.AuthResponse
import com.xdev.osm_mobile.horsligne.SessionManager
import com.xdev.osm_mobile.models.OneSignalManager
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
                val response = RetrofitClient.instance.login(
                    grantType = "TOKEN",
                    username = username,
                    password = password
                )

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        sessionManager.saveAuthTokens(
                            accessToken = authResponse.accessToken,
                            refreshToken = authResponse.refreshToken
                        )
                        authResponse.osmUser?.let { user ->
                            sessionManager.saveUser(user)
                            OneSignalManager.login(user.id)
                        }
                        _loginResult.value = Result.success(authResponse)
                    } else {
                        _loginResult.value = Result.failure(Exception("Réponse vide du serveur"))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Échec de la connexion"
                    _loginResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: IOException) {
                _loginResult.value = Result.failure(Exception("Erreur réseau : Vérifiez votre connexion internet"))
            } catch (e: Exception) {
                _loginResult.value = Result.failure(Exception("Une erreur inattendue est survenue : ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
