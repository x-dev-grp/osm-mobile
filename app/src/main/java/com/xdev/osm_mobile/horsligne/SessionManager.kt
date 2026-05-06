package com.xdev.osm_mobile.horsligne

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xdev.osm_mobile.models.User
import com.xdev.osm_mobile.utils.Constants

class SessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    companion object {
        @Volatile
        private var instance: SessionManager? = null
        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
    fun saveAuthTokens(accessToken: String, refreshToken: String?) {
        prefs.edit().apply {
            putString(Constants.KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(Constants.KEY_REFRESH_TOKEN, it) }
            putBoolean(Constants.KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    fun saveUser(user: User) {
        prefs.edit().apply {
            putString(Constants.KEY_USER_ID, user.id)
            putString(Constants.KEY_USERNAME, user.username)
            putBoolean(Constants.KEY_IS_NEW_USER, user.isNewUser)
            putString(Constants.KEY_USER_ROLE, user.role)
            putString("tenant_id", user.tenantId)
            val permissionsJson = gson.toJson(user.permissions)
            putString(Constants.KEY_USER_PERMISSIONS, permissionsJson)
            Log.d("SessionManager", "saveUser called: id=${user.id}, username=${user.username}")
            apply()
        }
    }
    fun getTenantId(): String? {
        return prefs.getString("tenant_id", null)
    }
    fun getUserRole(): String? {
        return prefs.getString(Constants.KEY_USER_ROLE, null)
    }
    fun getUserPermissions(): List<String> {
        val permissionsJson = prefs.getString(Constants.KEY_USER_PERMISSIONS, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(permissionsJson, type)
    }
    fun getAccessToken(): String? {
        return prefs.getString(Constants.KEY_ACCESS_TOKEN, null)
    }
    fun getRefreshToken(): String? {
        return prefs.getString(Constants.KEY_REFRESH_TOKEN, null)
    }
    fun getUsername(): String? {
        return prefs.getString(Constants.KEY_USERNAME, null)
    }
    fun getUserId(): String? {
        return prefs.getString(Constants.KEY_USER_ID, null)
    }
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false)
    }
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
