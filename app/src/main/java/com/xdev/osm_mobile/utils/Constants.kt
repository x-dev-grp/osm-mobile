package com.xdev.osm_mobile.utils

/**
 * [Constants] - Toutes les constantes de l'application
 *
 * RÔLE MÉTIER :
 * Centralise toutes les valeurs qui ne changent pas mais qui sont
 * cruciales pour le fonctionnement métier de l'application.
 */
object Constants {
    // ===== CONFIGURATION SERVEUR =====
    /**
     * BASE_URL : Adresse du serveur backend
     *
     * IMPORTANT MÉTIER :
     * - 10.0.2.2 = adresse spéciale pour l'émulateur Android (accès au localhost du PC)
     * - En production, ce sera l'IP réelle du serveur (ex: https://api.osm.com)
     * - Le port 8088 doit correspondre à celui configuré dans application.yml du backend
     */
    const val BASE_URL = "http://10.0.2.2:8084/"

    // ===== GESTION DE SESSION =====
    /**
     * Clés pour les SharedPreferences (stockage sécurisé des données de session)
     *
     * Ces données sont essentielles pour :
     * - Maintenir la connexion entre les redémarrages de l'app
     * - Éviter de redemander le mot de passe à chaque ouverture
     * - Personnaliser l'interface selon l'utilisateur
     */

    // ===== CRÉDENTIALS OAuth2 =====
    /**
     * Identifiants OAuth2 - SÉCURITÉ IMPORTANTE
     *
     * Ces valeurs doivent correspondre exactement à celles du backend :
     * - application.yml : oauth2.client.id et oauth2.client.secret
     * - ClientRegistry.kt : Les mêmes valeurs
     *
     * Elles identifient l'application mobile auprès du serveur.
     */
    const val CLIENT_ID = "osm-client"
    const val CLIENT_SECRET = "X7kP9mN2vQ8rT4wY6zA1bC3dE5fG8hJ9"
    const val PREF_NAME = "osm_mobile_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_USER_ID = "user_id"
    const val KEY_USERNAME = "username"
    const val KEY_IS_NEW_USER = "is_new_user"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_USER_PERMISSIONS = "user_permissions"
}