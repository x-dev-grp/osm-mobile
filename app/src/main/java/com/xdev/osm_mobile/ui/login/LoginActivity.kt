package com.xdev.osm_mobile.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.xdev.osm_mobile.MainActivity
import com.xdev.osm_mobile.OSMApplication
import com.xdev.osm_mobile.databinding.ActivityLoginBinding
import com.xdev.osm_mobile.ui.resetpassword.ResetPasswordActivity

/**
 * [LoginActivity] - Écran de connexion à l'application OSM
 *
 * RÔLE MÉTIER :
 * C'est la porte d'entrée de l'application. L'utilisateur doit s'authentifier
 * pour accéder aux fonctionnalités de l'application. Cet écran gère :
 * - La saisie des identifiants (nom d'utilisateur et mot de passe)
 * - La validation des champs
 * - La communication avec le serveur via le ViewModel
 * - La redirection vers l'écran principal ou de réinitialisation
 *
 * SÉCURITÉ :
 * - Vérification que le compte n'est pas verrouillé (géré par le backend)
 * - Mots de passe masqués à l'écran
 * - Session maintenue après connexion réussie
 */
class LoginActivity : AppCompatActivity() {

    // Binding pour accéder aux vues du layout activity_login.xml
    private lateinit var binding: ActivityLoginBinding

    // ViewModel qui gère la logique métier de connexion
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Vérification de session existante
        // MÉTIER : Si l'utilisateur est déjà connecté, on le redirige directement
        // vers l'écran principal sans lui demander de se reconnecter
        if (OSMApplication.sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setupUI()
        setupObservers()
    }
    private fun setupUI() {
        // Action de connexion - L'utilisateur clique sur le bouton "Login"
        binding.btnLogin.setOnClickListener {
            // Récupération des saisies utilisateur
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validation locale avant envoi au serveur
            if (validateInput(username, password)) {
                // Appel au ViewModel pour authentifier l'utilisateur
                viewModel.login(username, password, OSMApplication.sessionManager)
            }
        }

        // Action "Mot de passe oublié" - L'utilisateur a oublié son mot de passe
        binding.tvForgotPassword.setOnClickListener {
            // Redirection vers le flux de réinitialisation de mot de passe
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }
    }


    //Le nom d'utilisateur ne peut pas être vide
    //Le mot de passe ne peut pas être vide

    //Cette validation locale évite des appels inutiles au serveur
    private fun validateInput(username: String, password: String): Boolean {
        return when {
            username.isEmpty() -> {
                binding.etUsername.error = "Username required"
                false
            }

            password.isEmpty() -> {
                binding.etPassword.error = "Password required"
                false
            }

            else -> true
        }
    }

    /**
     * Observation des LiveData du ViewModel
     * MÉTIER : Réagit aux changements d'état de la connexion
     */
    private fun setupObservers() {
        // État de chargement - Affiche/masque l'indicateur de progression
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.progressBar.isVisible = isLoading
        }

        // Résultat de la tentative de connexion
        viewModel.loginResult.observe(this) { result ->
            result.onSuccess {
                // SUCCÈS MÉTIER : Authentification réussie
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { exception ->
                // ÉCHEC MÉTIER : Authentification refusée
                // Les causes possibles :
                // - Identifiants incorrects (401)
                // - Compte verrouillé (403)
                // - Problème réseau
                Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Navigation vers l'écran principal
     * MÉTIER : Après authentification réussie, l'utilisateur accède
     * aux fonctionnalités de l'application
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Ferme l'écran de login (empêche le retour avec "back")
    }
}