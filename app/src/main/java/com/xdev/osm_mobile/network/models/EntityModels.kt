package com.xdev.osm_mobile.network.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LotDto(
    @SerializedName("id") val id: String?,
    @SerializedName("lotNumber") val lotNumber: String?,
    @SerializedName("deliveryNumber") val deliveryNumber: String?,
    @SerializedName("oilQuantity") val oilQuantity: Double?,
    @SerializedName("oliveQuantity") val oliveQuantity: Double?,
    @SerializedName("deliveryDate") val deliveryDate: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("supplier") val supplier: SupplierMinimalDto?
) : Serializable

data class SupplierMinimalDto(
    @SerializedName("name") val name: String?
) : Serializable

data class ColisDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("capacityInLiters") val capacityInLiters: Double?,
    @SerializedName("stockQuantity") val stockQuantity: Int?,
    @SerializedName("buyPrice") val buyPrice: Double?,
    @SerializedName("sellingPrice") val sellingPrice: Double?,
    @SerializedName("description") val description: String?
) : Serializable

data class PaletteDto(
    @SerializedName("id") val id: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("status") val status: String?
) : Serializable

data class OfDto(
    @SerializedName("id") val id: String?,
    @SerializedName("ofNumber") val ofNumber: String?,
    @SerializedName("plannedDate") val plannedDate: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("quantityToProduce") val quantityToProduce: Double?
) : Serializable

data class OrderFabricationDTO(
    @SerializedName("id") val id: String?,
    @SerializedName("code") val code: String?,                     // numéro OF
    @SerializedName("statut") val statut: String?,                 // PLANIFIE, ACTIF, TERMINE, ANNULE
    @SerializedName("dateDebutPrevue") val dateDebutPrevue: String?,
    @SerializedName("dateFinPrevue") val dateFinPrevue: String?,
    @SerializedName("dateDebutReelle") val dateDebutReelle: String?,
    @SerializedName("dateFinReelle") val dateFinReelle: String?,
    @SerializedName("quantiteCible") val quantiteCible: Double?,
    @SerializedName("quantiteBonne") val quantiteBonne: Double?,
    @SerializedName("quantiteDefectueuse") val quantiteDefectueuse: Double?,
    @SerializedName("sku") val sku: SKUDto?,
    @SerializedName("ligneConditionnement") val ligneConditionnement: LigneConditionnementDto?
) : Serializable

data class SKUDto(
    @SerializedName("id") val id: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("volume") val volume: Double?,
    @SerializedName("category") val category: String?,
    @SerializedName("unitesParColis") val unitesParColis: Int?,
    @SerializedName("colisParPalette") val colisParPalette: Int?,
    @SerializedName("actif") val actif: Boolean?
) : Serializable