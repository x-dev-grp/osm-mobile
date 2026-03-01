package com.xdev.osm_mobile.ui.resetpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.network.models.UpdatePasswordRequest
import com.xdev.osm_mobile.network.models.User
import kotlinx.coroutines.launch

/**
 * ViewModel pour le flux "Mot de passe oublié"
 * Gère les 3 étapes :
 * 1. Demande de code (resetPassword)
 * 2. Validation du code (validateCode)
 * 3. Mise à jour du mot de passe (updatePassword)
 */
class ResetPasswordViewModel : ViewModel() {

    // ===== RÉSULTATS DES 3 ÉTAPES =====

    // Résultat de l'étape 1 : demande de code
    // Contient l'utilisateur (avec son ID) si succès
    private val _resetResult = MutableLiveData<Result<User>>()
    val resetResult: LiveData<Result<User>> = _resetResult

    // Résultat de l'étape 2 : validation du code
    // Unit = pas de données, juste succès/échec
    private val _validateResult = MutableLiveData<Result<Unit>>()
    val validateResult: LiveData<Result<Unit>> = _validateResult

    // Résultat de l'étape 3 : mise à jour du mot de passe
    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    // État de chargement (true = requête en cours)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // ===== ÉTAPE 1 : DEMANDER UN CODE =====

    /**
     * Étape 1 : L'utilisateur demande un code de réinitialisation
     * @param identifier Email ou numéro de téléphone
     */
    fun resetPassword(identifier: String) {
        viewModelScope.launch {  // Tâche en arrière-plan
            _isLoading.value = true

            try {
                // Appel au serveur
                val response = RetrofitClient.instance.resetPassword(identifier)

                if (response.isSuccessful && response.body() != null) {
                    // SUCCÈS : le code a été envoyé
                    _resetResult.value = Result.success(response.body()!!)
                } else {
                    // ERREUR : problème avec l'identifiant
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid identifier"      // Format incorrect
                        403 -> "Account is locked"        // Compte verrouillé
                        404 -> "User not found"           // Email inconnu
                        else -> "Reset failed: ${response.message()}"
                    }
                    _resetResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                // ERREUR RÉSEAU (pas de connexion, timeout...)
                _resetResult.value = Result.failure(Exception("Network error: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ===== ÉTAPE 2 : VALIDER LE CODE =====

    /**
     * Étape 2 : L'utilisateur valide le code reçu
     * @param userId ID de l'utilisateur (reçu de l'étape 1)
     * @param code Code à 6 chiffres reçu par email/SMS
     */
    fun validateCode(userId: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.instance.validateResetCode(userId, code)

                if (response.isSuccessful) {
                    // SUCCÈS : le code est correct
                    _validateResult.value = Result.success(Unit)
                } else {
                    // ERREUR : code incorrect ou expiré
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid or expired code"
                        else -> "Validation failed: ${response.message()}"
                    }
                    _validateResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                _validateResult.value = Result.failure(Exception("Network error: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ===== ÉTAPE 3 : CHANGER LE MOT DE PASSE =====

    /**
     * Étape 3 : L'utilisateur crée son nouveau mot de passe
     * @param userId ID de l'utilisateur
     * @param newPassword Nouveau mot de passe
     * @param confirmPassword Confirmation (doit être identique)
     */
    fun updatePassword(userId: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Vérification locale (évite un appel serveur inutile)
                if (newPassword != confirmPassword) {
                    _updateResult.value = Result.failure(Exception("Passwords do not match"))
                    _isLoading.value = false
                    return@launch
                }

                // Prépare la requête
                val request = UpdatePasswordRequest(
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )

                // Appel au serveur
                val response = RetrofitClient.instance.updatePassword(userId, request)

                if (response.isSuccessful) {
                    // SUCCÈS : mot de passe changé
                    _updateResult.value = Result.success(Unit)
                } else {
                    // ERREUR : mot de passe trop faible ou autre
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid password"
                        else -> "Update failed: ${response.message()}"
                    }
                    _updateResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                _updateResult.value = Result.failure(Exception("Network error: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}