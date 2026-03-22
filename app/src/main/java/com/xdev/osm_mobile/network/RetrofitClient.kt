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

    //échanges réseau (requêtes/réponses)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Assure que chaque appel API est authentifié correctement
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        val path = original.url.encodedPath

        if (path.contains("oauth2/token")) {
            // Requête d'authentification : utilisation de l'authentification Basic (client_id / client_secret)
            val credentials = Credentials.basic(Constants.CLIENT_ID, Constants.CLIENT_SECRET)
            requestBuilder.header("Authorization", credentials)
        } else {
            // Requête métier : ajout du jeton d'accès Bearer (si disponible)
            val token = com.xdev.osm_mobile.OSMApplication.sessionManager.getAccessToken()
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            // Ajout de l'identifiant du tenant (si disponible)
            val tenantId = com.xdev.osm_mobile.OSMApplication.sessionManager.getTenantId()
            if (!tenantId.isNullOrBlank()) {
                requestBuilder.header("X-Tenant-Id", tenantId)
            }
        }

        // Toujours demander une réponse JSON
        requestBuilder.header("Accept", "application/json")
        chain.proceed(requestBuilder.build())
    }

    // Authenticator qui gère les erreurs 401 (Unauthorized) en tentant de rafraîchir le jeton d'accès
    private val tokenAuthenticator = object : okhttp3.Authenticator {
        override fun authenticate(
            route: okhttp3.Route?,
            response: okhttp3.Response
        ): okhttp3.Request? {
            // Évite les boucles infinies si le refresh échoue également (après 3 tentatives)
            if (response.priorResponse?.priorResponse != null) {
                return null
            }

            synchronized(this) {
                val sessionManager = com.xdev.osm_mobile.OSMApplication.sessionManager
                val refreshToken = sessionManager.getRefreshToken()
                val currentToken = sessionManager.getAccessToken()

                if (refreshToken.isNullOrBlank()) return null

                // Si le jeton dans la session a déjà changé, on réessaie simplement avec le nouveau
                val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")
                if (!currentToken.isNullOrBlank() && requestToken != currentToken) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                // Exécution synchrone du rafraîchissement du jeton
                // On crée un client temporaire avec l'intercepteur de log et l'authentification Basic pour l'appel de refresh
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

                        // Retourne la requête originale avec le nouveau jeton
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

    // Client OkHttp configuré avec les intercepteurs, l'authenticator et les timeouts
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator) // Gestion 401 via refresh token
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Instance de l'interface ApiService (point d'entrée pour les appels réseau)
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}