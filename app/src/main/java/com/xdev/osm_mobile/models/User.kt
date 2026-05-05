package com.xdev.osm_mobile.models

import com.google.gson.annotations.SerializedName
data class User(
    @SerializedName(value = "id", alternate = ["userId"])
    val id: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("isNewUser")
    val isNewUser: Boolean = false,

    @SerializedName("role")
    val role: String = "User",

    @SerializedName("tenantId")
    val tenantId: String? = null,

    @SerializedName("permissions")
    val permissions: List<String> = emptyList()
)
