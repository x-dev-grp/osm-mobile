package com.xdev.osm_mobile.network

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
        level = HttpLoggingInterceptor.Level.BASIC
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
            if (!tenantId.isNullOrBlank()) {
                requestBuilder.header("X-Tenant-Id", tenantId)
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
                return null
            }

            synchronized(this) {
                val sessionManager = com.xdev.osm_mobile.OSMApplication.sessionManager
                val refreshToken = sessionManager.getRefreshToken()
                val currentToken = sessionManager.getAccessToken()
                if (refreshToken.isNullOrBlank()) return null
                val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")
                if (!currentToken.isNullOrBlank() && requestToken != currentToken) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }
                val refreshClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "Authorization",
                                okhttp3.Credentials.basic(
                                    Constants.CLIENT_ID,
                                    Constants.CLIENT_SECRET
                                )
                            )
                            .build()
                        chain.proceed(request)
                    }
                    .build()
                val refreshService = Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(refreshClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)

                try {
                    val refreshResponse =
                        refreshService.refreshTokenSync("refresh_token", refreshToken).execute()
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val auth = refreshResponse.body()!!
                        sessionManager.saveAuthTokens(auth.accessToken, auth.refreshToken)
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer ${auth.accessToken}")
                            .build()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(ArticleConfig::class.java, ArticleConfigDeserializer())
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