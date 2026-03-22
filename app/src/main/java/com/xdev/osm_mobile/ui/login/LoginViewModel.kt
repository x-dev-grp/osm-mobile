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

    // _isLoading : État interne (privé) pour suivre si une requête est en cours
    private val _isLoading = MutableLiveData(false)

    // isLoading : État exposé en lecture seule pour que la vue puisse afficher un spinner
    val isLoading: LiveData<Boolean> = _isLoading

    // _loginResult : Stocke le résultat de la tentative de connexion (Succès ou Échec)
    private val _loginResult = MutableLiveData<Result<AuthResponse>>()

    // loginResult : Observable par la vue pour déclencher la navigation ou afficher un message d'erreur
    val loginResult: LiveData<Result<AuthResponse>> = _loginResult


    fun login(username: String, password: String, sessionManager: SessionManager) {
        // viewModelScope : Lance une coroutine liée au cycle de vie du ViewModel.
        // Si l'utilisateur quitte l'écran, la requête réseau sera automatiquement annulée.
        viewModelScope.launch {
            // Début du chargement : on active l'indicateur visuel dans l'interface
            _isLoading.value = true

            try {
                // Appel API asynchrone via Retrofit pour obtenir les jetons d'accès
                val response = RetrofitClient.instance.login(
                    grantType = "TOKEN",
                    username = username,
                    password = password
                )

                // Vérifie si la réponse du serveur est un succès (code HTTP 200 OK)
                if (response.isSuccessful) {

                    // Récupération des données d'authentification (tokens + infos utilisateur)
                    val authResponse = response.body()

                    if (authResponse != null) {
                        // Debug : Affiche la réponse brute dans les logs (à désactiver en production)
                        println("AuthResponse: $authResponse")
                        // Save tokens
                        sessionManager.saveAuthTokens(
                            accessToken = authResponse.accessToken,
                            refreshToken = authResponse.refreshToken
                        )

                        // Si l'objet utilisateur est présent, on le sauvegarde également dans la session
                        authResponse.osmUser?.let { user ->
                            sessionManager.saveUser(user)
                        }

                        // On notifie la vue que la connexion est réussie
                        _loginResult.value = Result.success(authResponse)

                    } else {
                        // Cas où le corps de la réponse est vide malgré un code 200
                        _loginResult.value = Result.failure(Exception("Réponse vide du serveur"))
                    }

                } else {
                    // Le serveur a renvoyé une erreur (ex: 401 Identifiants incorrects, 500 Erreur serveur)
                    // On tente d'extraire le message d'erreur spécifique du corps de la réponse
                    val errorMsg = response.errorBody()?.string() ?: "Échec de la connexion"
                    _loginResult.value = Result.failure(Exception(errorMsg))
                }

            } catch (e: IOException) {
                // Erreur de connexion physique (ex: avion, pas de WiFi, serveur injoignable)
                _loginResult.value =
                    Result.failure(Exception("Erreur réseau : Vérifiez votre connexion internet"))

            } catch (e: Exception) {
                // Capture toute autre erreur inattendue (ex: erreur de parsing JSON ou crash logique)
                _loginResult.value =
                    Result.failure(Exception("Une erreur inattendue est survenue : ${e.message}"))

            } finally {
                // Dans tous les cas (succès ou erreur), on cache l'indicateur de chargement
                _isLoading.value = false
            }
        }
    }
}
