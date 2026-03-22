package com.xdev.osm_mobile.network


import com.xdev.osm_mobile.network.models.ApiResponse
import com.xdev.osm_mobile.network.models.AuthResponse
import com.xdev.osm_mobile.network.models.ColisDto
import com.xdev.osm_mobile.network.models.LotDto
import com.xdev.osm_mobile.network.models.OrderFabricationDTO
import com.xdev.osm_mobile.network.models.PaletteDto
import com.xdev.osm_mobile.network.models.UpdatePasswordRequest
import com.xdev.osm_mobile.network.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ===== AUTHENTIFICATION =====
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

    // ===== GESTION DES MOTS DE PASSE =====
    @POST("api/security/user/auth/resetPassword")
    suspend fun resetPassword(
        @Query("identifier") identifier: String
    ): Response<User>

    @POST("api/security/user/auth/validateResetCode/{userId}")
    suspend fun validateResetCode(
        @Path("userId") userId: String,
        @Query("code") code: String
    ): Response<Void>

    @POST("api/security/user/auth/updatePassword/{userId}")
    suspend fun updatePassword(
        @Path("userId") userId: String,
        @Body request: UpdatePasswordRequest
    ): Response<Void>


    // ===== LOTS =====
    @GET("api/reception/delivery/fetchAll")
    suspend fun getLots(): Response<ApiResponse<LotDto>>

    // ===== COLIS =====
    @GET("api/production/oil-container/fetchAll")
    suspend fun getColis(): Response<ApiResponse<ColisDto>>

    // ===== PALETTES =====
    @GET("api/production/palette/fetchAll")
    suspend fun getPalettes(): Response<ApiResponse<PaletteDto>>

    // ===== ORDRES DE FABRICATION (OF) =====
    @GET("api/ordreConditionement/of")
    suspend fun getOfs(): Response<ApiResponse<OrderFabricationDTO>>
}