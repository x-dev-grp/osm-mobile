package com.xdev.osm_mobile.network


import com.xdev.osm_mobile.network.models.ApiResponse
import com.xdev.osm_mobile.network.models.AuthResponse
import com.xdev.osm_mobile.network.models.ColisDto
import com.xdev.osm_mobile.network.models.LotDto
import com.xdev.osm_mobile.network.models.OrderFabricationDTO
import com.xdev.osm_mobile.network.models.PaletteDto
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    //authen
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
}
