package com.xdev.osm_mobile.utils.horsligne

import java.util.UUID


enum class SyncStatus {
    PENDING,
    SYNCED,
    ERROR
}


data class OfflineScan(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: String = "QR_CODE",
    val format: String = "QR_CODE",
    val timestamp: Long = System.currentTimeMillis(),
    val status: SyncStatus = SyncStatus.PENDING,
    val errorMessage: String? = null
)