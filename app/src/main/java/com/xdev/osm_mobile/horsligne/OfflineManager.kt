package com.xdev.osm_mobile.horsligne


import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xdev.osm_mobile.models.OfflineOperation
import com.xdev.osm_mobile.models.OfflineScan
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.models.ScanData
import com.xdev.osm_mobile.models.SyncRequest
import com.xdev.osm_mobile.models.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OfflineManager(private val context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "offline_operations_secure",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val gson = Gson()
    companion object {
    private const val KEY_OFFLINE_OPS = "offline_ops"
    private const val KEY_OFFLINE_SCANS = "offline_scans"
    private var instance: OfflineManager? = null

        fun getInstance(context: Context): OfflineManager {
            if (instance == null) {
                instance = OfflineManager(context.applicationContext)
            }
            return instance!!
        }
    }

    private fun getAllOperations(): MutableList<OfflineOperation> {
        val json = prefs.getString(KEY_OFFLINE_OPS, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<OfflineOperation>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveAllOperations(ops: List<OfflineOperation>) {
        val json = gson.toJson(ops)
        prefs.edit().putString(KEY_OFFLINE_OPS, json).apply()
    }
    fun saveOperation(operation: OfflineOperation) {
        val ops = getAllOperations()
        ops.add(operation)
        saveAllOperations(ops)
    }

    fun getPendingOperations(): List<OfflineOperation> =
        getAllOperations().filter { it.status == SyncStatus.PENDING }

    fun getAllOperationsWithStatus(): List<OfflineOperation> = getAllOperations()

    fun updateOperationStatus(id: String, newStatus: SyncStatus, errorMessage: String? = null) {
        val ops = getAllOperations().toMutableList()
        val index = ops.indexOfFirst { it.id == id }
        if (index != -1) {
            ops[index] = ops[index].copy(status = newStatus, errorMessage = errorMessage)
            saveAllOperations(ops)
        }
    }

    fun hasPendingOperations(): Boolean = getPendingOperations().isNotEmpty()
    fun getPendingOperationsCount(): Int = getPendingOperations().size


    private fun getAllScans(): MutableList<OfflineScan> {
        val json = prefs.getString(KEY_OFFLINE_SCANS, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<OfflineScan>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveAllScans(scans: List<OfflineScan>) {
        val json = gson.toJson(scans)
        prefs.edit().putString(KEY_OFFLINE_SCANS, json).apply()
    }

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
            Log.d("OfflineManager", "Envoi du scan ${scan.id} au serveur...")
            val syncRequest = SyncRequest(
                operationId = scan.id,
                url = "/api/ordreConditionement/mobile/scan",
                method = "POST",
                body = scan.content
            )
            val response = RetrofitClient.instance.syncOperation(syncRequest)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("OfflineManager", "Erreur sync scan: ${e.message}")
            false
        }
    }

    private suspend fun sendOperationToServer(op: OfflineOperation): Boolean {
        return try {
            Log.d("OfflineManager", "Envoi opération ${op.id} au serveur...")
            val syncRequest = SyncRequest(
                operationId = op.operationId,
                url = op.url,
                method = op.method,
                body = op.body
            )
            val response = RetrofitClient.instance.syncOperation(syncRequest)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("OfflineManager", " Erreur sync op: ${e.message}")
            false
        }
    }
    fun clearScansByStatus(status: SyncStatus) {
        val scans = getAllScans().filter { it.status != status }
        saveAllScans(scans)
    }
    fun clearOperationsByStatus(status: SyncStatus) {
        val ops = getAllOperations().filter { it.status != status }
        saveAllOperations(ops)
    }
    fun clearAllPendingScans() {
        clearScansByStatus(SyncStatus.PENDING)
    }
    fun syncPendingOperations(onComplete: (successCount: Int, failedCount: Int) -> Unit) {
        if (!NetworkUtils.isInternetAvailable(context)) {
            onComplete(0, getPendingOperationsCount())
            return
        }
        val pending = getPendingOperations()
        if (pending.isEmpty()) {
            onComplete(0, 0)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            var failedCount = 0

            for (op in pending) {
                try {
                    val isSuccess = sendOperationToServer(op)
                    if (isSuccess) {
                        withContext(Dispatchers.Main) {
                            updateOperationStatus(op.id, SyncStatus.SYNCED)
                        }
                        successCount++
                    } else {
                        withContext(Dispatchers.Main) {
                            updateOperationStatus(op.id, SyncStatus.ERROR, "Erreur serveur")
                        }
                        failedCount++
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        updateOperationStatus(op.id, SyncStatus.ERROR, e.message)
                    }
                    failedCount++
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(successCount, failedCount)
            }
        }
    }
}
