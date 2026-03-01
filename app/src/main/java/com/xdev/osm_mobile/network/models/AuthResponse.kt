package com.xdev.osm_mobile.network.models

import com.google.gson.annotations.SerializedName

/**
 * AuthResponse — OAuth2 token response from the server.
 *
 * Expected JSON shape (matching Angular expectations):
 * {
 *   "access_token": "eyJ...",
 *   "refresh_token": "dGh...",
 *   "token_type": "Bearer",
 *   "expires_in": 3600,
 *   "scope": "read write",
 *   "osmUser": {
 *     "username": "john.doe",
 *     "role": "Admin" or "User",
 *     "permissions": ["user:view", "user:edit", ...]
 *   }
 * }
 */
data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String?,

    @SerializedName("token_type")
    val tokenType: String?,

    @SerializedName("expires_in")
    val expiresIn: Long?,

    @SerializedName("scope")
    val scope: String?,

    @SerializedName("osmUser")  // Note: Changed from "osm_user" to match Angular
    val osmUser: User?  // The user object containing role and permissions
)