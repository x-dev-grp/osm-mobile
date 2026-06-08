package com.xdev.osm_mobile.horsligne

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NetworkMonitor(private val context: Context) {
    fun isConnected(): Boolean = NetworkUtils.isInternetAvailable(context)

    fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        cm.registerDefaultNetworkCallback(callback)
        trySend(isConnected())
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}