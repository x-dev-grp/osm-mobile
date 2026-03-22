package com.xdev.osm_mobile.utils.horsligne


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Classe utilitaire pour vérifier la disponibilité de la connexion internet
 */
object NetworkUtils {

    /**
     * Vérifie si une connexion internet est disponible
     * @param context Context de l'application
     * @return true si connecté à internet, false sinon
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Pour Android 6.0 (M) et versions antérieures
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }

        // Pour Android 6.0 (M) et versions ultérieures
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}