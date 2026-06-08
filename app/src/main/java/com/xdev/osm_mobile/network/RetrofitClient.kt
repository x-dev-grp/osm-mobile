package com.xdev.osm_mobile.network

import android.util.Log
import com.google.gson.GsonBuilder
import com.xdev.osm_mobile.models.ArticleConfig
import com.xdev.osm_mobile.models.ArticleConfigDeserializer
import com.xdev.osm_mobile.utils.Constants
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        val path = original.url.encodedPath

        if (path.contains("oauth2/token")) {
            val credentials = Credentials.basic(Constants.CLIENT_ID, Constants.CLIENT_SECRET)
            requestBuilder.header("Authorization", credentials)
        } else {
            val token = com.xdev.osm_mobile.OSMApplication.sessionManager.getAccessToken()
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            val tenantId = com.xdev.osm_mobile.OSMApplication.sessionManager.getTenantId()
            Log.d("RetrofitClient", "X-Tenant-Id envoyé: '$tenantId' pour $path")
            if (!tenantId.isNullOrBlank()) {
                requestBuilder.header("X-Tenant-Id", tenantId)
            } else {
                Log.w("RetrofitClient", "X-Tenant-Id est NULL ou VIDE — le backend retournera []")
            }
        }

        requestBuilder.header("Accept", "application/json")
        chain.proceed(requestBuilder.build())
    }
    private val tokenAuthenticator = object : okhttp3.Authenticator {
        override fun authenticate(
            route: okhttp3.Route?,
            response: okhttp3.Response
        ): okhttp3.Request? {
            if (response.priorResponse?.priorResponse != null) {
                Log.e("RetrofitClient", "Token refresh échoué après 2 tentatives")
                return null
            }
            synchronized(this) {
                val sessionManager = com.xdev.osm_mobile.OSMApplication.sessionManager
                val refreshToken = sessionManager.getRefreshToken()
                val currentToken = sessionManager.getAccessToken()
                if (refreshToken.isNullOrBlank()) {
                    Log.e("RetrofitClient", "Pas de refresh token disponible")
                    return null
                }
                val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")
                if (!currentToken.isNullOrBlank() && requestToken != currentToken) {
                    Log.d("RetrofitClient", "Token déjà rafraîchi, réutilisation")
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }
                val refreshClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor { innerChain ->
                        val request = innerChain.request().newBuilder()
                            .header(
                                "Authorization",
                                Credentials.basic(Constants.CLIENT_ID, Constants.CLIENT_SECRET)
                            )
                            .build()
                        innerChain.proceed(request)
                    }
                    .build()

                val refreshService = Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(refreshClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)

                return try {
                    val refreshResponse =
                        refreshService.refreshTokenSync("refresh_token", refreshToken).execute()

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val auth = refreshResponse.body()!!
                        sessionManager.saveAuthTokens(auth.accessToken, auth.refreshToken)
                        Log.d("RetrofitClient", "Token rafraîchi avec succès")
                        val tenantId = sessionManager.getTenantId()
                        val newRequest = response.request.newBuilder()
                            .header("Authorization", "Bearer ${auth.accessToken}")
                            .apply {
                                if (!tenantId.isNullOrBlank()) {
                                    header("X-Tenant-Id", tenantId)
                                }
                            }
                            .build()
                        newRequest
                    } else {
                        Log.e("RetrofitClient", "Refresh token refusé: ${refreshResponse.code()}")
                        null
                    }
                } catch (e: Exception) {
                    Log.e("RetrofitClient", "Exception lors du refresh token", e)
                    null
                }
            }
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(ArticleConfig::class.java, ArticleConfigDeserializer())
        .serializeNulls()
        .create()
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}