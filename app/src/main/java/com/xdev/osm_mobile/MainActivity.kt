package com.xdev.osm_mobile

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.xdev.osm_mobile.databinding.ActivityMainBinding
import com.xdev.osm_mobile.ui.login.LoginActivity

/**
 * Écran principal de l'application
 * Page d'accueil après connexion réussie
 *
 * Rôle :
 * - Afficher un message de bienvenue
 * - Vérifier si c'est la première connexion (changement de mot de passe obligatoire)
 * - Gérer la déconnexion via le menu
 */
class MainActivity : AppCompatActivity() {

    // Binding pour accéder aux éléments du layout
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()  // Configure l'interface
    }

    /**
     * Configure l'interface utilisateur
     * Vérifie d'abord si l'utilisateur doit changer son mot de passe
     */
    private fun setupUI() {

        // Afficher le message de bienvenue avec le nom de l'utilisateur
        binding.tvWelcome.text =
            "Welcome, ${OSMApplication.sessionManager.getUsername() ?: "User"}!"
    }

    /**
     * Crée le menu (options en haut à droite)
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Gère les clics sur les éléments du menu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()  // Déconnexion
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Déconnexion de l'utilisateur
     * - Efface la session
     * - Retourne à l'écran de connexion
     * - Empêche le retour en arrière
     */
    private fun logout() {
        OSMApplication.sessionManager.clearSession()  // Efface les tokens
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}