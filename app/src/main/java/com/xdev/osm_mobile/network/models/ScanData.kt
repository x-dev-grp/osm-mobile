package com.xdev.osm_mobile.network.models

import java.util.UUID

/**
 * Représente les données d'un scan à synchroniser
 */
data class ScanData(
    val id: String = UUID.randomUUID().toString(), // ID unique pour identifier le scan
    val content: String,      // Contenu scanné
    val type: String,         // Type de scan (QR_CODE, BARCODE, etc.)
    val format: String,       // Format spécifique
    val timestamp: Long,      // Timestamp du scan
    var synced: Boolean = false // Indique si déjà synchronisé
)