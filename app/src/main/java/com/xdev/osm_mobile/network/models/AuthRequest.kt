package com.xdev.osm_mobile.network.models  // ✅ CORRECT: osm_mobile

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("grant_type")
    val grantType: String = "TOKEN",

    @SerializedName("client_id")
    val clientId: String = "osm-client",

    @SerializedName("client_secret")
    val clientSecret: String = "X7kP9mN2vQ8rT4wY6zA1bC3dE5fG8hJ9",

    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("scope")
    val scope: String = "read write"
)