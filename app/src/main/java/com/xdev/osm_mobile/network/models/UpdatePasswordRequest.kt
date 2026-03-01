package com.xdev.osm_mobile.network.models

import com.google.gson.annotations.SerializedName

data class UpdatePasswordRequest(
    @SerializedName("newPassword")
    val newPassword: String,

    @SerializedName("confirmPassword")
    val confirmPassword: String
)
