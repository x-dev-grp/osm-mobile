package com.xdev.osm_mobile.database

import android.util.Log
import com.google.gson.Gson
import com.xdev.osm_mobile.database.dao.MainDao
import com.xdev.osm_mobile.database.entities.*
import com.xdev.osm_mobile.models.ArticleSecDto
import com.xdev.osm_mobile.models.OrderFabricationDTO
import com.xdev.osm_mobile.network.ApiService
import kotlinx.coroutines.flow.Flow

class AppRepository(private val apiService: ApiService, private val mainDao: MainDao) {
    private val gson = Gson()
    suspend fun findLotByNumber(number: String): LotEntity? = mainDao.getLotByNumber(number)
    val allOfs: Flow<List<OfEntity>> = mainDao.getAllOfs()
    suspend fun getOfById(id: String): OfEntity? = mainDao.getOfById(id)
    suspend fun findOfByCode(code: String): OfEntity? = mainDao.getOfByCode(code)
    suspend fun findOfByQrHex(qrHex: String): OfEntity? = mainDao.getOfByQrHex(qrHex)

    suspend fun refreshOfs() {
        try {
            val response = apiService.getOfs()
            Log.d("AppRepository", "refreshOfs → HTTP ${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("AppRepository", "Body est null: ${body == null} | taille: ${body?.size ?: 0}")
                if (!body.isNullOrEmpty()) {
                    Log.d("AppRepository", "Premier OF brut: ${gson.toJson(body.first())}")
                } else {
                    Log.w("AppRepository", "Le serveur a retourné une liste VIDE []")
                    Log.w("AppRepository", "   Vérifiez le X-Tenant-Id dans RetrofitClient (voir log 'RetrofitClient')")
                }
                val ofs = body?.mapNotNull { dto ->
                    val id = dto.id ?: run {
                        Log.w("AppRepository", "OF ignoré car id == null: ${gson.toJson(dto)}")
                        return@mapNotNull null
                    }
                    OfEntity(
                        id = id,
                        code = dto.code,
                        statut = dto.statut,
                        skuCode = dto.skuCode,
                        skuId = dto.skuId,
                        bomId = dto.bomId,
                        qrHex = dto.qrHex,
                        ligneId = dto.ligneId,
                        ligneNom = dto.lotVracNom ,
                        lotVracId = dto.lotVracId,
                        quantiteCible = dto.quantiteCible,
                        quantiteBonne = dto.quantiteBonne,
                        quantiteNC = dto.quantiteNC,
                        dateDebutPrevue = dto.dateDebutPrevue,
                        dateFinPrevue = dto.dateFinPrevue,
                        dateDebutReelle = dto.dateDebutReelle,
                        dateFinReelle = dto.dateFinReelle,
                        motifNC = dto.motifNC,
                        qualityStatus = null,
                        fullJson = gson.toJson(dto)
                    )
                } ?: emptyList()
                Log.d("AppRepository", "OFs mappés en entités Room: ${ofs.size}")
                mainDao.deleteAllOfs()
                mainDao.insertOfs(ofs)
                Log.d("AppRepository", "insertOfs terminé")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AppRepository", "Erreur HTTP ${response.code()}: $errorBody")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Exception dans refreshOfs", e)
        }
    }
    suspend fun refreshOfDetail(id: String) {
        try {
            val response = apiService.getOfById(id)
            if (response.isSuccessful) {
                val dto = response.body() ?: return
                val entity = OfEntity(
                    id = dto.id ?: return,
                    code = dto.code,
                    statut = dto.statut,
                    skuCode = dto.skuCode,
                    skuId = dto.skuId,
                    bomId = dto.bomId,
                    ligneId = dto.ligneId,
                    ligneNom = dto.ligneNom,
                    lotVracId = dto.lotVracId,
                    quantiteCible = dto.quantiteCible,
                    quantiteBonne = dto.quantiteBonne,
                    quantiteNC = dto.quantiteNC,
                    dateDebutPrevue = dto.dateDebutPrevue,
                    dateFinPrevue = dto.dateFinPrevue,
                    dateDebutReelle = dto.dateDebutReelle,
                    dateFinReelle = dto.dateFinReelle,
                    motifNC = dto.motifNC,
                    qualityStatus = null,
                    fullJson = gson.toJson(dto),
                    qrHex = dto.qrHex,
                )
                mainDao.insertOf(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun getArticleById(id: String): ArticleEntity? = mainDao.getArticleById(id)
    suspend fun findArticleByQrHex(qrHex: String): ArticleEntity? = mainDao.getArticleByQrHex(qrHex)
    suspend fun refreshArticle(id: String) {
        try {
            val response = apiService.getArticleById(id)
            if (response.isSuccessful) {
                val dto = response.body() ?: return
                val entity = ArticleEntity(
                    id = dto.id ?: return,
                    nom = dto.nom,
                    categorie = dto.categorie,
                    um = dto.um,
                    qrHex = dto.qrHex,
                    stockMinimum = dto.stockMinimum,
                    stockMaximum = dto.stockMaximum,
                    actif = dto.actif,
                    fournisseurId = dto.fournisseur?.id,
                    fournisseurNom = dto.fournisseur?.nom,
                    fullJson = gson.toJson(dto)
                )
                mainDao.insertArticles(listOf(entity))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun refreshAllArticles() {
        try {
            val response = apiService.getArticles()
            if (response.isSuccessful) {
                val articles = response.body()?.mapNotNull { dto ->
                    val id = dto.id ?: return@mapNotNull null
                    ArticleEntity(
                        id = id,
                        nom = dto.nom,
                        categorie = dto.categorie,
                        um = dto.um,
                        qrHex = dto.qrHex,
                        stockMinimum = dto.stockMinimum,
                        stockMaximum = dto.stockMaximum,
                        actif = dto.actif,
                        fournisseurId = dto.fournisseur?.id,
                        fournisseurNom = dto.fournisseur?.nom,
                        fullJson = gson.toJson(dto)
                    )
                } ?: emptyList()
                mainDao.deleteAllArticles()
                mainDao.insertArticles(articles)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    val allStocks: Flow<List<StockEntity>> = mainDao.getAllStocks()
    suspend fun getStockByArticle(articleId: String): StockEntity? = mainDao.getStockByArticle(articleId)
    suspend fun refreshStock(articleId: String) {
        try {
            val response = apiService.getStockByArticle(articleId)
            if (response.isSuccessful) {
                val dto = response.body() ?: return
                val emp = dto.emplacement
                val entity = StockEntity(
                    id = dto.id ?: return,
                    articleId = dto.articleId ?: articleId,
                    quantiteActuelle = dto.quantiteActuelle,
                    emplacementId = emp?.id,
                    emplacementCode = emp?.code,
                    emplacementNom = emp?.nom,
                    emplacementZone = emp?.zone,
                    emplacementType = emp?.typeEmplacement,
                    emplacementDisponible = emp?.disponible,
                    emplacementCapaciteMaximale = null,
                    emplacementCapaciteActuelle = null,
                    emplacementConditionsSpeciales = null,
                    emplacementTemperatureMin = null,
                    emplacementTemperatureMax = null,
                    reservePour = null,
                    reserveDate = null
                )
                mainDao.insertStock(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun getAllMovements(): Flow<List<MovementEntity>> = mainDao.getAllMovements()
    suspend fun refreshAllMovements() {
        try {
            val response = apiService.getAllStockMovements()
            if (response.isSuccessful) {
                val movements = response.body()?.mapNotNull { dto ->
                    dto.id?.let { id ->
                        MovementEntity(
                            id = id,
                            articleId = dto.article?.id ?: return@mapNotNull null,
                            articleName = dto.article?.nom,
                            quantity = dto.quantite ?: 0,
                            typeMouvement = dto.typeMouvement ?: "UNKNOWN",
                            motif = dto.motif,
                            dateMouvement = dto.dateMouvement
                        )
                    }
                } ?: emptyList()
                mainDao.deleteAllMovements()
                mainDao.insertMovements(movements)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun refreshMovementsForArticle(articleId: String) {
        try {
            val response = apiService.getStockMovements(articleId)
            if (response.isSuccessful) {
                val movements = response.body()?.mapNotNull { dto ->
                    dto.id?.let { id ->
                        MovementEntity(
                            id = id,
                            articleId = articleId,
                            articleName = dto.article?.nom,
                            quantity = dto.quantite ?: 0,
                            typeMouvement = dto.typeMouvement ?: "UNKNOWN",
                            motif = dto.motif,
                            dateMouvement = dto.dateMouvement
                        )
                    }
                } ?: emptyList()
                mainDao.insertMovements(movements)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun getAllStocksWithArticle(): Flow<List<MainDao.StockWithArticle>> =
        mainDao.getAllStocksWithArticle()
    suspend fun refreshAllStocks() {
        try {
            val response = apiService.getAllStocks()
            if (response.isSuccessful) {
                val stocks = response.body()?.mapNotNull { dto ->
                    dto.id?.let { id ->
                        StockEntity(
                            id = id,
                            articleId = dto.articleId ?: return@mapNotNull null,
                            quantiteActuelle = dto.quantiteActuelle,
                            emplacementId = dto.emplacement?.id,
                            emplacementCode = dto.emplacement?.code,
                            emplacementNom = dto.emplacement?.nom,
                            emplacementZone = dto.emplacement?.zone,
                            emplacementType = dto.emplacement?.typeEmplacement,
                            emplacementDisponible = dto.emplacement?.disponible,
                            emplacementCapaciteMaximale = null,
                            emplacementCapaciteActuelle = null,
                            emplacementConditionsSpeciales = null,
                            emplacementTemperatureMin = null,
                            emplacementTemperatureMax = null,
                            reservePour = null,
                            reserveDate = null
                        )
                    }
                } ?: emptyList()
                mainDao.deleteAllStocks()
                mainDao.insertStocks(stocks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    val allExpeditions: Flow<List<ExpeditionEntity>> = mainDao.getAllExpeditions()

    suspend fun findExpeditionById(id: String): ExpeditionEntity? = mainDao.getExpeditionById(id)

    suspend fun findExpeditionByPublicCode(publicCode: String): ExpeditionEntity? =
        mainDao.getExpeditionByPublicCode(publicCode)
    suspend fun refreshAllExpeditions() {
        try {
            val response = apiService.getExpeditions()
            if (response.isSuccessful) {
                val expeditions = response.body()?.mapNotNull { dto ->
                    val id = dto.id ?: return@mapNotNull null
                    ExpeditionEntity(
                        id = id,
                        expeditionNumber = dto.expeditionNumber,
                        projetId = dto.projetId,
                        projetCode = dto.projetCode,
                        status = dto.status,
                        destination = dto.destination,
                        plannedShipDate = dto.plannedShipDate,
                        validatedAt = dto.validatedAt,
                        shippedAt = dto.shippedAt,
                        carrierName = dto.carrierName,
                        driverName = dto.driverName,
                        truckNumber = dto.truckNumber,
                        totalQuantity = dto.totalQuantity,
                        totalVolume = dto.totalVolume,
                        publicCode = dto.publicCode,
                        notes = dto.notes,
                        fullJson = gson.toJson(dto)
                    )
                } ?: emptyList()
                mainDao.deleteAllExpeditions()
                mainDao.insertExpeditions(expeditions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun refreshExpedition(id: String) {
        try {
            val response = apiService.getExpeditionById(id)
            if (response.isSuccessful) {
                val dto = response.body() ?: return
                val entity = ExpeditionEntity(
                    id = dto.id ?: return,
                    expeditionNumber = dto.expeditionNumber,
                    projetId = dto.projetId,
                    projetCode = dto.projetCode,
                    status = dto.status,
                    destination = dto.destination,
                    plannedShipDate = dto.plannedShipDate,
                    validatedAt = dto.validatedAt,
                    shippedAt = dto.shippedAt,
                    carrierName = dto.carrierName,
                    driverName = dto.driverName,
                    truckNumber = dto.truckNumber,
                    totalQuantity = dto.totalQuantity,
                    totalVolume = dto.totalVolume,
                    publicCode = dto.publicCode,
                    notes = dto.notes,
                    fullJson = gson.toJson(dto)
                )
                mainDao.insertExpedition(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun updateLocalConsumption(ofId: String, articleId: String, quantiteReelle: Double, motif: String) {
        val of = mainDao.getOfById(ofId) ?: return
        val gson = Gson()
        val dto = gson.fromJson(of.fullJson, OrderFabricationDTO::class.java)
        val lignes = dto.lignes?.toMutableList() ?: return
        val index = lignes.indexOfFirst { it.articleId == articleId }
        if (index >= 0) {
            lignes[index] = lignes[index].copy(quantiteReelle = quantiteReelle, motifAjustement = motif)
            dto.lignes = lignes
            val updatedJson = gson.toJson(dto)
            mainDao.updateOfPartial(ofId, updatedJson)
        }
    }}