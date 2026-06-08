package com.xdev.osm_mobile.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lots")
data class LotEntity(
    @PrimaryKey val id: String,
    val lotNumber: String?,
    val deliveryNumber: String?,
    val oilQuantity: Double?,
    val status: String?,
    val supplierName: String?,
    val deliveryDate: String?
)

@Entity(tableName = "ordres_fabrication")
data class OfEntity(
    @PrimaryKey val id: String,
    val code: String?,
    val statut: String?,
    val skuCode: String?,
    val skuId: String?,
    val bomId: String?,
    val ligneId: String?,
    val ligneNom: String?,
    val lotVracId: String?,
    val quantiteCible: Double?,
    val quantiteBonne: Double?,
    val quantiteNC: Double?,
    val dateDebutPrevue: String?,
    val dateFinPrevue: String?,
    val dateDebutReelle: String?,
    val dateFinReelle: String?,
    val motifNC: String?,
    val qualityStatus: String?,
    val fullJson: String?,
    val qrHex: String?,
)

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val nom: String?,
    val categorie: String?,
    val um: String?,
    val qrHex: String?,
    val stockMinimum: Int?,
    val stockMaximum: Int?,
    val actif: Boolean?,
    val fournisseurId: String?,
    val fournisseurNom: String?,
    val fullJson: String?
)
@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val id: String,
    val articleId: String,
    val quantiteActuelle: Int?,
    val emplacementId: String?,
    val emplacementCode: String?,
    val emplacementNom: String?,
    val emplacementZone: String?,
    val emplacementType: String?,
    val emplacementDisponible: Boolean?,
    val emplacementCapaciteMaximale: String?,
    val emplacementCapaciteActuelle: String?,
    val emplacementConditionsSpeciales: String?,
    val emplacementTemperatureMin: Double?,
    val emplacementTemperatureMax: Double?,
    val reservePour: String?,
    val reserveDate: String?
)
@Entity(tableName = "boms")
data class BomEntity(
    @PrimaryKey val id: String,
    val skuId: String?,
    val version: String?
)

@Entity(tableName = "bom_lines")
data class BomLineEntity(
    @PrimaryKey val id: String,
    val bomId: String?,
    val articleId: String?,
    val articleNom: String?,
    val quantity: Double?,
    val unitOfMeasure: String?
)
@Entity(tableName = "expeditions")
data class ExpeditionEntity(
    @PrimaryKey val id: String,
    val expeditionNumber: String?,
    val projetId: String?,
    val projetCode: String?,
    val status: String?,
    val destination: String?,
    val plannedShipDate: String?,
    val validatedAt: String?,
    val shippedAt: String?,
    val carrierName: String?,
    val driverName: String?,
    val truckNumber: String?,
    val totalQuantity: Int?,
    val totalVolume: Double?,
    val publicCode: String?,
    val notes: String?,
    val fullJson: String?
)
@Entity(tableName = "movements")
data class MovementEntity(
    @PrimaryKey val id: String,
    val articleId: String,
    val articleName: String?,
    val quantity: Int,
    val typeMouvement: String,
    val motif: String?,
    val dateMouvement: String?
)