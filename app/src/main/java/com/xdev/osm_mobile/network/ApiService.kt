package com.xdev.osm_mobile.network


import com.xdev.osm_mobile.models.ApiResponse
import com.xdev.osm_mobile.models.ArticleSecDto
import com.xdev.osm_mobile.models.AuthResponse
import com.xdev.osm_mobile.models.BomDto
import com.xdev.osm_mobile.models.ColisDto
import com.xdev.osm_mobile.models.LotDto
import com.xdev.osm_mobile.models.OrderFabricationDTO
import com.xdev.osm_mobile.models.PaletteDto
import com.xdev.osm_mobile.models.QCControlPointDTO
import com.xdev.osm_mobile.models.QCResultDTO
import com.xdev.osm_mobile.models.QrResolveResponse
import com.xdev.osm_mobile.models.RegistrationRequest
import com.xdev.osm_mobile.models.StockSecDto
import com.xdev.osm_mobile.models.SyncRequest
import com.xdev.osm_mobile.models.UserDeviceDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun login(
        @Field("grant_type") grantType: String,
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<AuthResponse>

    @FormUrlEncoded
    @POST("oauth2/token")
    fun refreshTokenSync(
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String
    ): retrofit2.Call<AuthResponse>

    @GET("api/reception/delivery/fetchAll")
    suspend fun getLots(): Response<ApiResponse<LotDto>>


    @GET("api/production/oil-container/fetchAll")
    suspend fun getColis(): Response<ApiResponse<ColisDto>>


    @GET("api/production/palette/fetchAll")
    suspend fun getPalettes(): Response<ApiResponse<PaletteDto>>

    @GET("/api/ordreConditionement/of/all")
    suspend fun getOfs(): Response<List<OrderFabricationDTO>>

    @GET("/api/ordreConditionement/of/{id}")
    suspend fun getOfById(@Path("id") id: String): Response<OrderFabricationDTO>

    @POST("/api/ordreConditionement/mobile/sync")
    suspend fun syncOperation(@Body request: SyncRequest): Response<Unit>

    @GET("/api/ordreConditionement/qualite/plans/of/{ofId}/points/active")
    suspend fun getActiveControlPoints(@Path("ofId") ofId: String): Response<ApiResponse<QCControlPointDTO>>

    @POST("/api/ordreConditionement/qualite/resultats/add")
    suspend fun submitQCResult(@Body result: QCResultDTO): Response<ApiResponse<QCResultDTO>>

    @GET("/api/ordreConditionement/qualite/resultats/of/{ofId}/historique")
    suspend fun getQCHistory(@Path("ofId") ofId: String): Response<ApiResponse<QCResultDTO>>
    @GET("/api/ordreConditionement/of/resolve/{publicCode}")
    suspend fun resolveOF(@Path("publicCode") publicCode: String): Response<QrResolveResponse>
    @GET("/api/inventaire/articles/resolve/{publicCode}")
    suspend fun resolveArticle(@Path("publicCode") publicCode: String): Response<QrResolveResponse>
    @GET("/api/inventaire/articles/{id}")
    suspend fun getArticleById(@Path("id") id: String): Response<ArticleSecDto>
    @GET("/api/inventaire/stocks/article/{articleId}")
    suspend fun getStockByArticle(@Path("articleId") articleId: String): Response<StockSecDto>

    @PUT("/api/inventaire/stocks/{articleId}/entree")
    suspend fun entreeStock(
        @Path("articleId") articleId: String,
        @Body request: StockMovementRequest
    ): Response<StockSecDto>

    @PUT("/api/inventaire/stocks/{articleId}/sortie")
    suspend fun sortieStock(
        @Path("articleId") articleId: String,
        @Body request: StockMovementRequest
    ): Response<StockSecDto>

    @PUT("/api/inventaire/stocks/{articleId}/ajuster")
    suspend fun ajusterStock(
        @Path("articleId") articleId: String,
        @Body request: StockAdjustmentRequest
    ): Response<StockSecDto>

    @PUT("/api/ordreConditionement/of/{id}/ajustements")
    suspend fun ajusterConsommation(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<OrderFabricationDTO>


    @POST("/api/security/user/register-device")
    suspend fun registerDevice(
        @Body request: RegistrationRequest
    ): Response<UserDeviceDto>
    @GET("/api/inventaire/articles")
    suspend fun getArticles(): Response<List<ArticleSecDto>>

    @GET("/api/inventaire/boms/{id}")
    suspend fun getBomById(@Path("id") id: String): Response<BomDto>







    // modifie
    data class StockMovementRequest(val quantite: Int, val motif: String)
    data class StockAdjustmentRequest(val quantite: Int, val motif: String) // nouvelle quantité absolue

}
