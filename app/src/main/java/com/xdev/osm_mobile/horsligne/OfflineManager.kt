package com.xdev.osm_mobile.horsligne

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xdev.osm_mobile.models.OfflineOperation
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.models.SyncRequest
import com.xdev.osm_mobile.models.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class OfflineManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("offline_operations", Context.MODE_PRIVATE
        )
    private val gson = Gson()
    companion object {
        private const val KEY_OFFLINE_OPS = "offline_ops"
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
    fun deleteOperation(id: String) {
        val ops = getAllOperations().toMutableList()
        if (ops.removeAll { it.id == id }) {
            saveAllOperations(ops)
        }
    }
    fun hasPendingOperations(): Boolean = getPendingOperations().isNotEmpty()

    fun getPendingOperationsCount(): Int = getPendingOperations().size

    fun clearOperationsByStatus(status: SyncStatus) {
        val ops = getAllOperations().filter { it.status != status }
        saveAllOperations(ops)
    }
    fun clearAllOperations() {
        saveAllOperations(emptyList())
    }
    fun clearAllSyncItems() {
        clearAllOperations()
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
            Log.e("OfflineManager", "Erreur sync op: ${e.message}")
            false
        }
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