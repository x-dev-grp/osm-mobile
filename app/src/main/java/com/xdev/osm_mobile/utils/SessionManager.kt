package com.xdev.osm_mobile.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xdev.osm_mobile.network.models.User

class SessionManager private constructor(context: Context) {

    //Initialisation du stockage
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        @Volatile
        private var instance: SessionManager? = null

        //si il ya une instance retourne instance si nn cree une nouvelle
        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    //pour sauv les token
    fun saveAuthTokens(accessToken: String, refreshToken: String?) {
        prefs.edit().apply {
            putString(Constants.KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(Constants.KEY_REFRESH_TOKEN, it) }
            putBoolean(Constants.KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    //sauv les donner de l'utilisateur
    fun saveUser(user: User) {
        prefs.edit().apply {
            putString(Constants.KEY_USER_ID, user.id)
            putString(Constants.KEY_USERNAME, user.username)
            putBoolean(Constants.KEY_IS_NEW_USER, user.isNewUser)
            putString(Constants.KEY_USER_ROLE, user.role)
            putString("tenant_id", user.tenantId)

            // Save permissions as JSON string
            val permissionsJson = gson.toJson(user.permissions)
            putString(Constants.KEY_USER_PERMISSIONS, permissionsJson)

            apply()
        }
    }

    fun getTenantId(): String? {
        return prefs.getString("tenant_id", null)
    }

    //pur lire le role de l'utilisateur
    fun getUserRole(): String? {
        return prefs.getString(Constants.KEY_USER_ROLE, null)
    }

    // lecture des permissions de l'utilisateur
    fun getUserPermissions(): List<String> {
        val permissionsJson = prefs.getString(Constants.KEY_USER_PERMISSIONS, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(permissionsJson, type)
    }

    //acces token
    fun getAccessToken(): String? {
        return prefs.getString(Constants.KEY_ACCESS_TOKEN, null)
    }

    //refresh token
    fun getRefreshToken(): String? {
        return prefs.getString(Constants.KEY_REFRESH_TOKEN, null)
    }

    //id de l'utilisateur
    fun getUserId(): String? {
        return prefs.getString(Constants.KEY_USER_ID, null)
    }

    //nom d'utilisateur
    fun getUsername(): String? {
        return prefs.getString(Constants.KEY_USERNAME, null)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false)
    }

    fun isNewUser(): Boolean {
        return prefs.getBoolean(Constants.KEY_IS_NEW_USER, true)
    }

    fun hasPermission(permission: String): Boolean {
        val role = getUserRole()
        // Admin has all permissions (matching Angular behavior)
        if (role == "Admin") return true

        val permissions = getUserPermissions()
        return permissions.contains(permission)
    }

    fun hasModule(module: String): Boolean {
        val role = getUserRole()
        if (role == "Admin") return true

        val permissions = getUserPermissions()
        return permissions.any {
            it.substringBefore(":") == module
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}