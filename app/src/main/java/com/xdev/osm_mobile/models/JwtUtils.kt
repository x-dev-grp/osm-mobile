package com.xdev.osm_mobile.models

import android.util.Base64
import android.util.Log
import org.json.JSONObject

object JwtUtils {
    fun decode(token: String): JSONObject? {
        return try {
            var payload = token.split(".")[1]
            val remainder = payload.length % 4
            if (remainder > 0) payload += "=".repeat(4 - remainder)
            val decoded = Base64.decode(payload, Base64.URL_SAFE)
            JSONObject(String(decoded, Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e("JwtUtils", "Erreur décodage: ${e.message}")
            null
        }
    }
    fun getUserId(token: String): String? {
        val claims = decode(token) ?: return null
        // Essayer d'abord le claim direct (après CustomTokenCustomizer)
        val directId = claims.optString("user_id", null)
        if (!directId.isNullOrEmpty()) return directId

        // Sinon lire depuis osmUser (structure actuelle)
        return claims.optJSONObject("osmUser")?.optString("id", null)
    }

    fun getUsername(token: String): String? {
        val claims = decode(token) ?: return null
        return claims.optString("sub", null)
    }

    fun getUserRole(token: String): String? {
        val claims = decode(token) ?: return null
        // Essayer claim direct d'abord, sinon osmUser.role.roleName
        val directRole = claims.optString("user_role", null)
        if (!directRole.isNullOrEmpty()) return directRole
        return claims.optJSONObject("osmUser")
            ?.optJSONObject("role")
            ?.optString("roleName", null)
    }

    fun getTenantId(token: String): String? {
        val claims = decode(token) ?: return null
        val direct = claims.optString("tenant_id", null)
        if (!direct.isNullOrEmpty()) return direct
        return claims.optJSONObject("osmUser")?.optString("tenantId", null)
    }

    fun isNewUser(token: String): Boolean {
        val claims = decode(token) ?: return false
        if (claims.has("is_new_user")) return claims.optBoolean("is_new_user", false)
        return claims.optJSONObject("osmUser")?.optBoolean("isNewUser", false) ?: false
    }

    fun getPermissions(token: String): List<String> {
        val claims = decode(token) ?: return emptyList()
        // Essayer permissions directes
        val permsArray = claims.optJSONArray("permissions")
        if (permsArray != null) {
            return (0 until permsArray.length()).map { permsArray.getString(it) }
        }
        // Sinon utiliser authorities
        val authArray = claims.optJSONArray("authorities") ?: return emptyList()
        return (0 until authArray.length()).map { authArray.getString(it) }
    }
}
