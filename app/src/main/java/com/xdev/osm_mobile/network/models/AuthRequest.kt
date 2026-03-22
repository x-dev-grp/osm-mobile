package com.xdev.osm_mobile.network.models  // ✅ CORRECT: osm_mobile

import com.google.gson.annotations.SerializedName
import com.xdev.osm_mobile.utils.Constants

data class AuthRequest(
    @SerializedName("grant_type")
    val grantType: String = "TOKEN",

    @SerializedName("client_id")
    val clientId: String = "osm-client",

    @SerializedName("client_secret")
    val clientSecret: String = Constants.CLIENT_SECRET,

    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("scope")
    val scope: String = "read write"
)