package com.xdev.osm_mobile.network

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

    // Add Basic Authentication header
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()

        // Create Basic Auth credentials (same as Angular)
        // The authorization_header in Angular is likely "Basic base64(client:secret)"
        val credentials = Credentials.basic("osm-client", "X7kP9mN2vQ8rT4wY6zA1bC3dE5fG8hJ9")

        val requestBuilder = original.newBuilder()
            .header("Authorization", credentials)
            .header("Accept", "application/json")
        // Content-Type will be set by @FormUrlEncoded

        chain.proceed(requestBuilder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor) // Add auth interceptor
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}