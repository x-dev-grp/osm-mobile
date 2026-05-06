package com.xdev.osm_mobile.models

import java.util.UUID
data class ScanData(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: String,
    val format: String,
    val timestamp: Long = System.currentTimeMillis(),
    var synced: Boolean = false
)
