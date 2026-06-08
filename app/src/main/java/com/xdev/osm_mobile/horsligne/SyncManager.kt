package com.xdev.osm_mobile.horsligne

import android.util.Log
import com.xdev.osm_mobile.OSMApplication
import com.xdev.osm_mobile.database.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncManager(
    private val repository: AppRepository,
    private val networkMonitor: NetworkMonitor
) {
    private val appScope = CoroutineScope(Dispatchers.IO)
    private val tag = "SyncManager"
    suspend fun syncNow() {
        if (!networkMonitor.isConnected() || !OSMApplication.sessionManager.isLoggedIn()) {
            Log.d(tag, "Sync skipped: no network or not logged in")
            return
        }
        Log.d(tag, "Starting full sync...")
        withContext(Dispatchers.IO) {
            repository.refreshOfs()
            repository.refreshAllArticles()
            repository.refreshAllStocks()
            repository.refreshAllMovements()
            repository.refreshAllExpeditions()
        }
        Log.d(tag, "Full sync completed")
    }

    fun startAutoSync() {
        appScope.launch {
            networkMonitor.observeConnectivity().collectLatest { connected ->
                if (connected) {
                    Log.d(tag, "Network connected, triggering sync")
                    syncNow()
                }
            }
        }
    }
}