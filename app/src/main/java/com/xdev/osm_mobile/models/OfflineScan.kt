package com.xdev.osm_mobile.models

import java.util.UUID

data class OfflineScan(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: String = "QR_CODE",
    val format: String = "QR_CODE",
    val timestamp: Long = System.currentTimeMillis(),
    val status: SyncStatus = SyncStatus.PENDING,
    val errorMessage: String? = null
)
