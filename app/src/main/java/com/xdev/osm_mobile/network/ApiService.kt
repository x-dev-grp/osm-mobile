package com.xdev.osm_mobile.network  // ✅ CORRECT: osm_mobile

import com.xdev.osm_mobile.network.models.AuthResponse
import com.xdev.osm_mobile.network.models.UpdatePasswordRequest
import com.xdev.osm_mobile.network.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun login(
        @Field("grant_type") grantType: String,
        @Field("username") username: String,
        @Field("password") password: String
        // No client_id or client_secret - they're in the header now
    ): Response<AuthResponse>

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
}