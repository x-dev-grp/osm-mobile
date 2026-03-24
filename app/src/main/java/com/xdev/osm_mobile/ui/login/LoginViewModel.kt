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

/**
 * LoginViewModel : Responsable de la logique métier de l'écran de connexion.
 * Il fait le pont entre l'interface utilisateur (UI) et la couche réseau (Retrofit).
 */
class LoginViewModel : ViewModel() {


    private val _isLoading = MutableLiveData(false)

    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginResult = MutableLiveData<Result<AuthResponse>>()

    val loginResult: LiveData<Result<AuthResponse>> = _loginResult


    fun login(username: String, password: String, sessionManager: SessionManager) {
        //si utili quitte l'ecran avant la fin sa tache se terminer.


        viewModelScope.launch {

            _isLoading.value = true

            try {
                // appel retrofit pour jeton d acces
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


                        authResponse.osmUser?.let { user ->
                            sessionManager.saveUser(user)
                        }

                        // notifier conx réussie
                        _loginResult.value = Result.success(authResponse)

                    } else {

                        _loginResult.value = Result.failure(Exception("Réponse vide du serveur"))
                    }

                } else {

                    val errorMsg = response.errorBody()?.string() ?: "Échec de la connexion"
                    _loginResult.value = Result.failure(Exception(errorMsg))
                }

            } catch (e: IOException) {
                _loginResult.value =
                    Result.failure(Exception("Erreur réseau : Vérifiez votre connexion internet"))

            } catch (e: Exception) {

                _loginResult.value =
                    Result.failure(Exception("Une erreur inattendue est survenue : ${e.message}"))

            } finally {

                _isLoading.value = false
            }
        }
    }
}
