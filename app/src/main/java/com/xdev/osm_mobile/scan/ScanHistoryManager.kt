package com.xdev.osm_mobile.scan

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xdev.osm_mobile.scan.ScanItem

class ScanHistoryManager(private val context: Context) {
    //pour stocker les scan dans la memoire (local)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("scan_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getScanHistory(): List<ScanItem> { //recuperer l'historique
        val json = prefs.getString("history", "[]") ?: "[]"
        val type = object : TypeToken<List<ScanItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addScanToHistory(scanItem: ScanItem) {
        val history = getScanHistory().toMutableList()
        history.add(0, scanItem) // Ajouter au début
        // Limiter à 50 éléments
        if (history.size > 50) {
            history.removeAt(history.size - 1)
        }
        saveHistory(history)
    }

    fun clearHistory() {
        saveHistory(emptyList())
    }

    private fun saveHistory(history: List<ScanItem>) {
        val json = gson.toJson(history)
        prefs.edit().putString("history", json).apply()
    }
}