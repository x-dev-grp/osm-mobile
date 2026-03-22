package com.xdev.osm_mobile.network.models

import com.google.gson.annotations.SerializedName

data class OilTransactionDto(
    @SerializedName("id") val id: String?,
    @SerializedName("qualityGrade") val qualityGrade: String?,
    @SerializedName("quantityKg") val quantityKg: Double?,
    @SerializedName("unitPrice") val unitPrice: Double?,
    @SerializedName("totalPrice") val totalPrice: Double?,
    @SerializedName("transactionType") val transactionType: String?,
    @SerializedName("transactionState") val transactionState: String?,
    @SerializedName("oilType") val oilType: String?,
    @SerializedName("createdAt") val createdAt: String?
)
