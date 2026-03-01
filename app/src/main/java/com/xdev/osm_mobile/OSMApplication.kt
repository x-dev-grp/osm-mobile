package com.xdev.osm_mobile

import android.app.Application
import com.xdev.osm_mobile.utils.SessionManager

/**
 * Classe Application - S'exécute au démarrage de l'app
 *
 * Rôle :
 * - Initialiser les composants globaux (une seule fois)
 * - Rendre SessionManager accessible partout dans l'app
 * - Se lance avant la première activité
 */
class OSMApplication : Application() {

    companion object {
        // Variable accessible de partout : OSMApplication.sessionManager
        // lateinit = sera initialisée plus tard (pas besoin de valeur ici)
        lateinit var sessionManager: SessionManager
    }

    /**
     * Appelé au tout début, avant même la première activité
     * Initialise les services nécessaires
     */
    override fun onCreate() {
        super.onCreate()

        // Initialise le gestionnaire de session
        // getInstance(this) = crée une instance unique (singleton)
        sessionManager = SessionManager.getInstance(this)
    }
}