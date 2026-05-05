package com.xdev.osm_mobile.models

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

data class OrderFabricationDTO(
    @SerializedName("id") val id: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("statut") val statut: String?,
    @SerializedName("dateDebutPrevue") val dateDebutPrevue: String?,
    @SerializedName("dateFinPrevue") val dateFinPrevue: String?,
    @SerializedName("dateDebutReelle") val dateDebutReelle: String?,
    @SerializedName("dateFinReelle") val dateFinReelle: String?,
    @SerializedName("quantiteCible") val quantiteCible: Double?,
    @SerializedName("quantiteBonne") val quantiteBonne: Double?,
    @SerializedName("quantiteNC") val quantiteNC: Double?,
    @SerializedName("quantiteDefectueuse") val quantiteDefectueuse: Double?,
    @SerializedName("dureeReelle") val dureeReelle: Long?,
    @SerializedName("skuId") val skuId: String?,
    @SerializedName("skuCode") val skuCode: String?,
    @SerializedName("ligneId") val ligneId: String?,
    @SerializedName("ligneNom") val ligneNom: String?,
    @SerializedName("lotVracId") val lotVracId: String?,
    @SerializedName("bomId") val bomId: String?,
    @SerializedName("sku") val sku: SKUDto?,
    @SerializedName("lignes") val lignes: List<LigneOFDto>?,
    @SerializedName("motifNC") val motifNC: String? = null,
    @SerializedName("qrHex") val qrHex: String? = null
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
data class LigneOFDto(
    @SerializedName("id") val id: String?,
    @SerializedName("articleId") val articleId: String?,
    @SerializedName("articleNom") val articleNom: String?,
    @SerializedName("quantiteTheorique") val quantiteTheorique: Double?,
    @SerializedName("quantiteReelle") val quantiteReelle: Double?,
    @SerializedName("motifAjustement") val motifAjustement: String?
): Serializable
data class QCControlPointDTO(
    @SerializedName("id") val id: String?,
    @SerializedName("nom") val nom: String?,
    @SerializedName("type") val type: String?, // "NUMERIC", "BOOLEAN", "TEXT"
    @SerializedName("minValue") val minValue: Double?,
    @SerializedName("maxValue") val maxValue: Double?,
    @SerializedName("blocking") val blocking: Boolean? = false
) : Serializable
data class SaisieProductionRequest(
    @SerializedName("quantiteBonne") val quantiteBonne: Double,
    @SerializedName("quantiteNC") val quantiteNC: Double,
    @SerializedName("motifNC") val motifNC: String? = null
) : Serializable
data class BomDto(
    @SerializedName("id") val id: String?,
    @SerializedName("skuId") val skuId: String?,
    @SerializedName("version") val version: String?,
    @SerializedName("lines") val lines: List<BomLineDto>?
)

data class BomLineDto(
    @SerializedName("id") val id: String?,
    @SerializedName("articleId") val articleId: String?,
    @SerializedName("articleNom") val articleNom: String?,
    @SerializedName("quantity") val quantity: Double?,
    @SerializedName("unitOfMeasure") val unitOfMeasure: String?
)
data class ArticleSecDto(
    @SerializedName("id") val id: String?,
    @SerializedName("nom") val nom: String?,
    @SerializedName("categorie") val categorie: String?,
    @SerializedName("um") val um: String?,
    @SerializedName("actif") val actif: Boolean?,
    @SerializedName("stockMinimum") val stockMinimum: Int?,
    @SerializedName("stockMaximum") val stockMaximum: Int?,
    @SerializedName("fournisseur") val fournisseur: FournisseurDto?,
    @SerializedName("qrHex") val qrHex: String?,
    @SerializedName("qrUrl") val qrUrl: String?,
    @SerializedName("qrImageBase64") val qrImageBase64: String?,

    // Nouveau champ polymorphique
    @SerializedName("configuration")
    val configuration: ArticleConfig? = null
) : Serializable
data class FournisseurDto(
    @SerializedName("id") val id: String?,
    @SerializedName("nom") val nom: String?
) : Serializable
data class StockSecDto(
    @SerializedName("id") val id: String?,
    @SerializedName("quantiteActuelle") val quantiteActuelle: Int?,
    @SerializedName("articleId") val articleId: String?,
    @SerializedName("emplacement") val emplacement: EmplacementStockDto?
) : Serializable
data class EmplacementStockDto(
    @SerializedName("id") val id: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("nom") val nom: String?,
    @SerializedName("zone") val zone: String?,
    @SerializedName("typeEmplacement") val typeEmplacement: String?,
    @SerializedName("disponible") val disponible: Boolean?
) : Serializable
data class QrResolveResponse(
    @SerializedName("entityType") val entityType: String,
    @SerializedName("publicCode") val publicCode: String,
    @SerializedName("entityId") val entityId: String,
    @SerializedName("label") val label: String,
    @SerializedName("status") val status: String,
    @SerializedName("mobileRoute") val mobileRoute: String,
    @SerializedName("data") val data: Any? = null
) : Serializable
data class QCResultDTO(
    @SerializedName("id") val id: String?,
    @SerializedName("controlPointId") val controlPointId: String?,
    @SerializedName("ofId") val ofId: String?,
    @SerializedName("valeur") val valeur: String?,
    @SerializedName("statut") val statut: String?,
    @SerializedName("commentaire") val commentaire: String?,
    @SerializedName("photo") val photo: String?,
    @SerializedName("signature") val signature: String?,
    @SerializedName("dateControle") val dateControle: String?
)
data class SyncRequest(
    @SerializedName("operationId") val operationId: String,
    @SerializedName("url") val url: String,
    @SerializedName("method") val method: String,
    @SerializedName("body") val body: String
): Serializable
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
): Serializable
data class RegistrationRequest(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("playerId")
    val playerId: String
)
data class UserDeviceDto(
    val id: String?,
    val userId: String?,
    val playerId: String?
)
