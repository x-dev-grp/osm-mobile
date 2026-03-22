package com.xdev.osm_mobile.network.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("isNewUser")
    val isNewUser: Boolean = false,

    @SerializedName("role")
    val role: String = "User",  // Default role

    @SerializedName("tenantId")
    val tenantId: String? = null,

    @SerializedName("permissions")
    val permissions: List<String> = emptyList()  // User permissions
)

// Role enum to match Angular
enum class Role {
    Admin,
    User
}