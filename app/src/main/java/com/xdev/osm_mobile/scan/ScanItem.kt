package com.xdev.osm_mobile.scan

import android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class représentant un élément scanné
 * @param content Le contenu scanné (texte du QR code)
 * @param type Le type de scan (QR_CODE, BARCODE, etc.)
 * @param format Le format spécifique du code (optionnel)
 * @param timestamp Timestamp du scan en millisecondes
 */

data class ScanItem(
    val content: String,
    val type: String,
    val format: String = "QR_CODE",
    val timestamp: Long
) {
    /**
     * Retourne la date formatée pour l'affichage
     */
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Retourne une icône appropriée selon le type de contenu
     */
    fun getContentTypeIcon(): Int {
        return when {
            content.startsWith("OF:") || content.matches(Regex("\\d{8,}")) ->
                R.drawable.ic_menu_agenda

            content.startsWith("LOT:") ->
                R.drawable.ic_menu_manage

            content.startsWith("UNIT:") ->
                R.drawable.ic_popup_disk_full

            content.startsWith("COLIS:") ->
                R.drawable.ic_menu_view

            content.startsWith("PALETTE:") ->
                R.drawable.ic_menu_gallery

            else ->
                R.drawable.ic_menu_info_details
        }
    }

    /**
     * Retourne une couleur appropriée selon le type de contenu
     * Utilise les couleurs standard de Material Design
     */
    fun getContentTypeColor(): Int {
        return when {
            content.startsWith("OF:") || content.matches(Regex("\\d{8,}")) ->
                com.google.android.material.R.color.design_default_color_primary

            content.startsWith("LOT:") ->
                com.google.android.material.R.color.design_default_color_secondary

            content.startsWith("UNIT:") ->
                com.google.android.material.R.color.design_default_color_primary_variant

            content.startsWith("COLIS:") ->
                com.google.android.material.R.color.design_default_color_secondary_variant

            content.startsWith("PALETTE:") ->
                com.google.android.material.R.color.material_dynamic_neutral50

            else ->
                com.google.android.material.R.color.design_default_color_error
        }
    }

    /**
     * Retourne une description lisible du type de contenu
     */
    fun getContentTypeDescription(): String {
        return when {
            content.startsWith("OF:") || content.matches(Regex("\\d{8,}")) ->
                "Ordre de Fabrication"

            content.startsWith("LOT:") ->
                "Lot"

            content.startsWith("UNIT:") ->
                "Unité de stockage"

            content.startsWith("COLIS:") ->
                "Colis"

            content.startsWith("PALETTE:") ->
                "Palette"

            else ->
                "Scan générique"
        }
    }

    /**
     * Retourne la couleur de fond appropriée pour le type de contenu
     */
    fun getContentTypeBackgroundColor(): Int {
        return when {
            content.startsWith("OF:") || content.matches(Regex("\\d{8,}")) ->
                com.google.android.material.R.color.material_dynamic_primary10

            content.startsWith("LOT:") ->
                com.google.android.material.R.color.material_dynamic_secondary10

            content.startsWith("UNIT:") ->
                com.google.android.material.R.color.material_dynamic_tertiary10

            content.startsWith("COLIS:") ->
                com.google.android.material.R.color.material_dynamic_primary20

            content.startsWith("PALETTE:") ->
                com.google.android.material.R.color.material_dynamic_secondary20

            else ->
                com.google.android.material.R.color.material_dynamic_neutral10
        }
    }
}