package com.xdev.osm_mobile.utils.horsligne


import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xdev.osm_mobile.network.models.ScanData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OfflineManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("offline_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_OFFLINE_SCANS = "offline_scans"
        private var instance: OfflineManager? = null

        fun getInstance(context: Context): OfflineManager {
            if (instance == null) {
                instance = OfflineManager(context.applicationContext)
            }
            return instance!!
        }
    }

    // --- Méthodes privées de gestion de la liste ---
    private fun getAllScans(): MutableList<OfflineScan> {
        val json = prefs.getString(KEY_OFFLINE_SCANS, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<OfflineScan>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveAllScans(scans: List<OfflineScan>) {
        val json = gson.toJson(scans)
        prefs.edit().putString(KEY_OFFLINE_SCANS, json).apply()
    }

    // --- Méthodes publiques ---
    fun saveScanOffline(scanData: ScanData) {
        val offlineScan = OfflineScan(
            content = scanData.content,
            type = scanData.type,
            format = scanData.format,
            timestamp = scanData.timestamp,
            status = SyncStatus.PENDING
        )
        val scans = getAllScans()
        scans.add(offlineScan)
        saveAllScans(scans)
    }

    fun getAllScansWithStatus(): List<OfflineScan> = getAllScans()

    fun getPendingScans(): List<OfflineScan> =
        getAllScans().filter { it.status == SyncStatus.PENDING }

    fun updateScanStatus(scanId: String, newStatus: SyncStatus, errorMessage: String? = null) {
        val scans = getAllScans().toMutableList()
        val index = scans.indexOfFirst { it.id == scanId }
        if (index != -1) {
            val old = scans[index]
            scans[index] = old.copy(status = newStatus, errorMessage = errorMessage)
            saveAllScans(scans)
        }
    }

    fun hasPendingScans(): Boolean = getPendingScans().isNotEmpty()

    fun getPendingScansCount(): Int = getPendingScans().size

    /**
     * Synchronise tous les scans en attente avec le serveur.
     */
    fun syncPendingScans(onComplete: (successCount: Int, failedCount: Int) -> Unit) {
        if (!NetworkUtils.isInternetAvailable(context)) {
            onComplete(0, getPendingScansCount())
            return
        }

        val pendingScans = getPendingScans()
        if (pendingScans.isEmpty()) {
            onComplete(0, 0)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            var failedCount = 0

            for (scan in pendingScans) {
                try {
                    val isSuccess = sendScanToServer(scan)
                    if (isSuccess) {
                        withContext(Dispatchers.Main) {
                            updateScanStatus(scan.id, SyncStatus.SYNCED)
                        }
                        successCount++
                    } else {
                        withContext(Dispatchers.Main) {
                            updateScanStatus(scan.id, SyncStatus.ERROR, "Erreur serveur")
                        }
                        failedCount++
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        updateScanStatus(scan.id, SyncStatus.ERROR, e.message)
                    }
                    failedCount++
                }
            }

            withContext(Dispatchers.Main) {
                onComplete(successCount, failedCount)
            }
        }
    }

    private suspend fun sendScanToServer(scan: OfflineScan): Boolean {
        return try {
            // TODO: Remplacer par votre véritable appel API
            // val scanData = ScanData(...)
            // val response = RetrofitClient.instance.sendScan(scanData)
            // response.isSuccessful
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearScansByStatus(status: SyncStatus) {
        val scans = getAllScans().filter { it.status != status }
        saveAllScans(scans)
    }

    fun clearAllPendingScans() {
        clearScansByStatus(SyncStatus.PENDING)
    }
}