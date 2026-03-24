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


class LoginActivity : AppCompatActivity() {

    // Binding pour accéder aux vues du layout activity_login.xml
    private lateinit var binding: ActivityLoginBinding

    // ViewModel qui gère la logique métier de connexion
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // verif la session/ si connecter pagine direct vers la page main

        if (OSMApplication.sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setupUI()
        setupObservers()
    }
    private fun setupUI() {

        // bouton login
        binding.btnLogin.setOnClickListener {

            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Valid local
            if (validateInput(username, password)) {
                // Appel au ViewModel pour authentifier l'utilisateur
                viewModel.login(username, password, OSMApplication.sessionManager)
            }
        }

    }

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


    private fun setupObservers() {
        // État de chargement - Affiche/masque l'indicateur de progression
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.progressBar.isVisible = isLoading
        }

        // Résultat de la tentative de connexion
        viewModel.loginResult.observe(this) { result ->
            result.onSuccess {

            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { exception ->

            Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}