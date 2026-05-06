package com.xdev.osm_mobile.models

import java.util.UUID

enum class SyncStatus { PENDING, SYNCED, ERROR }
data class OfflineOperation(
    val id: String = UUID.randomUUID().toString(),
    val operationId: String,
    val url: String,
    val method: String,
    val body: String,
    val status: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
