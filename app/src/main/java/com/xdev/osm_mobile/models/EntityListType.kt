package com.xdev.osm_mobile.models

import java.util.Locale

enum class EntityListType(
    val code: String,
    val toolbarTitle: String,
    val displayName: String
) {
    LOT(
        code = "LOT",
        toolbarTitle = "Lots",
        displayName = "Lots"
    ),

    COLIS(
        code = "COLIS",
        toolbarTitle = "Colis",
        displayName = "Colis"
    ),

    PALETTE(
        code = "PALETTE",
        toolbarTitle = "Palettes",
        displayName = "Palettes"
    ),

    OF(
        code = "OF",
        toolbarTitle = "Ordres de Fabrication",
        displayName = "OF"
    );

    companion object {
        fun fromCode(value: String?): EntityListType? {
            val normalized = value?.trim()?.uppercase(Locale.getDefault()) ?: return null
            return values().firstOrNull { it.code == normalized }
        }
    }
}
