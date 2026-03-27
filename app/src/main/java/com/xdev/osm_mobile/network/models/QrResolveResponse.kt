package com.xdev.osm_mobile.network.models


import com.google.gson.annotations.SerializedName

data class QrResolveResponse(
    val entityType: String,
    val publicCode: String,
    val entityId: String,
    val label: String,
    val status: String,
    val mobileRoute: String,
    val data: Any? = null
)