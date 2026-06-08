package com.xdev.osm_mobile.database.dao

import androidx.room.*
import com.xdev.osm_mobile.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MainDao {

    @Query("SELECT * FROM lots")
    fun getAllLots(): Flow<List<LotEntity>>

    @Query("SELECT * FROM lots WHERE id = :id")
    suspend fun getLotById(id: String): LotEntity?

    @Query("SELECT * FROM lots WHERE lotNumber = :number")
    suspend fun getLotByNumber(number: String): LotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLots(lots: List<LotEntity>)

    @Query("DELETE FROM lots")
    suspend fun deleteAllLots()

    @Query("SELECT * FROM ordres_fabrication")
    fun getAllOfs(): Flow<List<OfEntity>>

    @Query("SELECT * FROM ordres_fabrication WHERE id = :id")
    suspend fun getOfById(id: String): OfEntity?

    @Query("SELECT * FROM ordres_fabrication WHERE code = :code")
    suspend fun getOfByCode(code: String): OfEntity?

    @Query("SELECT * FROM ordres_fabrication WHERE statut = :statut")
    fun getOfsByStatut(statut: String): Flow<List<OfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfs(ofs: List<OfEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOf(of: OfEntity)
    @Query("SELECT * FROM articles")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE categorie = :categorie")
    fun getArticlesByCategorie(categorie: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()

    @Query("SELECT * FROM stocks")
    fun getAllStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks WHERE articleId = :articleId")
    suspend fun getStockByArticle(articleId: String): StockEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)
    @Query("SELECT * FROM boms WHERE skuId = :skuId")
    suspend fun getBomBySkuId(skuId: String): BomEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBom(bom: BomEntity)
    @Query("SELECT * FROM bom_lines WHERE bomId = :bomId")
    suspend fun getBomLinesByBomId(bomId: String): List<BomLineEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBomLines(lines: List<BomLineEntity>)
    @Query("DELETE FROM bom_lines WHERE bomId = :bomId")
    suspend fun deleteBomLines(bomId: String)
    @Query("SELECT * FROM articles WHERE qrHex = :qrHex")
    suspend fun getArticleByQrHex(qrHex: String): ArticleEntity?
    @Query("SELECT * FROM ordres_fabrication WHERE qrHex = :qrHex")
    suspend fun getOfByQrHex(qrHex: String): OfEntity?

    @Query("SELECT * FROM expeditions ORDER BY plannedShipDate DESC")
    fun getAllExpeditions(): Flow<List<ExpeditionEntity>>

    @Query("SELECT * FROM expeditions WHERE id = :id")
    suspend fun getExpeditionById(id: String): ExpeditionEntity?

    @Query("SELECT * FROM expeditions WHERE status = :status ORDER BY plannedShipDate DESC")
    fun getExpeditionsByStatus(status: String): Flow<List<ExpeditionEntity>>

    @Query("SELECT * FROM expeditions WHERE publicCode = :publicCode")
    suspend fun getExpeditionByPublicCode(publicCode: String): ExpeditionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpeditions(expeditions: List<ExpeditionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpedition(expedition: ExpeditionEntity)

    @Query("DELETE FROM expeditions")
    suspend fun deleteAllExpeditions()
    @Query("DELETE FROM stocks")
    suspend fun deleteAllStocks()
    @Query("SELECT * FROM ordres_fabrication WHERE statut IN ('ACTIF', 'EN_COURS') ORDER BY dateDebutPrevue DESC")
    fun getActiveOFs(): Flow<List<OfEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)
    @Query("DELETE FROM ordres_fabrication")
    suspend fun deleteAllOfs()
    @Query("SELECT * FROM movements ORDER BY dateMouvement DESC")
    fun getAllMovements(): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE articleId = :articleId ORDER BY dateMouvement DESC")
    fun getMovementsByArticle(articleId: String): Flow<List<MovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<MovementEntity>)

    @Query("DELETE FROM movements")
    suspend fun deleteAllMovements()
    @Query("UPDATE ordres_fabrication SET fullJson = :fullJson WHERE id = :id")
    suspend fun updateOfPartial(id: String, fullJson: String)
    @Query("""
        SELECT s.id as stockId, s.articleId, a.nom as articleName, 
               s.quantiteActuelle, s.emplacementCode, s.emplacementNom,
               a.stockMinimum as articleStockMin, a.um as articleUm, a.categorie as articleCategorie
        FROM stocks s
        LEFT JOIN articles a ON s.articleId = a.id
        ORDER BY a.nom ASC
    """)
    fun getAllStocksWithArticle(): Flow<List<StockWithArticle>>

    data class StockWithArticle(
        val stockId: String,
        val articleId: String,
        val articleName: String?,
        val quantiteActuelle: Int?,
        val emplacementCode: String?,
        val emplacementNom: String?,
        val articleStockMin: Int?,
        val articleUm: String?,
        val articleCategorie: String?
    )
}
