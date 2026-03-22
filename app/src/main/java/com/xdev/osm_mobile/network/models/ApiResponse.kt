package com.xdev.osm_mobile.network.models

import com.google.gson.annotations.SerializedName

/**
 * Global API Response wrapper matching the backend BaseController response format
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: List<T>?
)
