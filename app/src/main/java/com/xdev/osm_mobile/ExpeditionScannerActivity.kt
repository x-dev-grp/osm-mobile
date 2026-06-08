package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.ExpeditionActionRequest
import com.xdev.osm_mobile.models.ExpeditionDto
import com.xdev.osm_mobile.models.OfflineOperation
import com.xdev.osm_mobile.models.QrResolveResponse
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ExpeditionScannerActivity : QRScannerActivity() {

    private val gson = Gson()
    private var expeditionMode: String = MODE_PICK
    private var preloadedExpeditionId: String? = null
    private var resolvedExpedition: ExpeditionDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        expeditionMode = intent.getStringExtra(EXTRA_EXPEDITION_MODE) ?: MODE_PICK
        preloadedExpeditionId = intent.getStringExtra(EXTRA_EXPEDITION_ID)

        supportActionBar?.title = when (expeditionMode) {
            MODE_LOAD -> "Scanner chargement"
            else -> "Scanner expédition"
        }
        if (expeditionMode == MODE_LOAD && preloadedExpeditionId != null) {
            fetchExpeditionAndShowDialog(preloadedExpeditionId!!)
        }
    }
    override fun navigateToEntity(response: QrResolveResponse) {
        when (response.entityType) {
            "EXPEDITION" -> fetchExpeditionAndShowDialog(response.entityId)
            "OF", "ARTICLE" -> {
                Toast.makeText(
                    this,
                    "Ce QR est un ${response.entityType}, pas une expédition.",
                    Toast.LENGTH_LONG
                ).show()
                resumeScanning()
            }
            else -> {
                Toast.makeText(this, "Type non reconnu : ${response.entityType}", Toast.LENGTH_LONG).show()
                resumeScanning()
            }
        }
    }
    private fun fetchExpeditionAndShowDialog(expeditionId: String) {
        lifecycleScope.launch {
            var hasDispatched = false
            if (NetworkUtils.isInternetAvailable(this@ExpeditionScannerActivity)) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.getExpeditionById(expeditionId)
                    }
                    if (response.isSuccessful && response.body() != null) {
                        resolvedExpedition = response.body()!!
                        launch(Dispatchers.IO) { OSMApplication.repository.refreshExpedition(expeditionId) }
                        dispatchDialog(resolvedExpedition!!)
                        hasDispatched = true
                    }
                } catch (e: Exception) {
                    Log.e("ExpeditionScanner", "Erreur réseau, fallback cache", e)
                }
            }
            if (!hasDispatched) {
                val cached = withContext(Dispatchers.IO) {
                    OSMApplication.repository.findExpeditionById(expeditionId)
                }
                if (cached != null) {
                    try {
                        resolvedExpedition = withContext(Dispatchers.IO) {
                            gson.fromJson(cached.fullJson ?: "{}", ExpeditionDto::class.java)
                        }
                        dispatchDialog(resolvedExpedition!!)
                        hasDispatched = true
                    } catch (e: Exception) {
                        Log.e("ExpeditionScanner", "Cache JSON corrompu", e)
                    }
                }
            }

            if (!hasDispatched) {
                Toast.makeText(this@ExpeditionScannerActivity, "Expédition introuvable (hors ligne)", Toast.LENGTH_LONG).show()
                resumeScanning()
            }
        }
    }

    private fun dispatchDialog(exp: ExpeditionDto) {
        when (expeditionMode) {
            MODE_LOAD -> showLoadDialog(exp)
            else -> showPickDialog(exp)
        }
    }
    private fun showPickDialog(exp: ExpeditionDto) {
        val status = exp.status?.uppercase()
        if (status != "DRAFT" && status != "READY") {
            AlertDialog.Builder(this)
                .setTitle("Picking impossible")
                .setMessage("Statut actuel : ${exp.status}. Seules les expéditions DRAFT ou READY peuvent être préparées.")
                .setPositiveButton("OK") { _, _ -> resumeScanning() }
                .show()
            return
        }

        if (status == "READY") {
            AlertDialog.Builder(this)
                .setTitle("Picking déjà terminé — ${exp.expeditionNumber}")
                .setMessage(buildString {
                    append("Destination : ${exp.destination ?: "-"}\n")
                    append("Total : ${exp.totalQuantity ?: 0} unités\n\n")
                    append("Articles :\n${buildLinesText(exp)}")
                })
                .setPositiveButton("OK") { _, _ -> finish() }
                .setOnDismissListener { finish() }
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmer picking — ${exp.expeditionNumber}")
            .setMessage(buildString {
                append("Destination : ${exp.destination ?: "-"}\n")
                append("Total : ${exp.totalQuantity ?: 0} unités\n\n")
                append("Articles :\n${buildLinesText(exp)}")
            })
            .setPositiveButton("Confirmer picking") { _, _ -> markReady(exp.id ?: "") }
            .setNegativeButton("Annuler") { _, _ -> resumeScanning() }
            .show()
    }

    private fun markReady(expeditionId: String) {
        val currentStatus = resolvedExpedition?.status?.uppercase()
        if (currentStatus != null && currentStatus != "DRAFT") {
            Toast.makeText(
                this,
                "Picking déjà effectué (statut : $currentStatus). Aucune action requise.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }
        if (!NetworkUtils.isInternetAvailable(this)) {
            saveOffline(expeditionId, "ready")
            return
        }
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.markExpeditionReady(
                        expeditionId,
                        ExpeditionActionRequest(comment = "Picking confirmé via scan mobile")
                    )
                }
                if (response.isSuccessful) {
                    launch(Dispatchers.IO) { OSMApplication.repository.refreshExpedition(expeditionId) }
                    Toast.makeText(this@ExpeditionScannerActivity, "✅ Expédition READY", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ExpeditionScannerActivity, "Erreur ${response.code()}", Toast.LENGTH_SHORT).show()
                    resumeScanning()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpeditionScannerActivity, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                resumeScanning()
            }
        }
    }

    private fun showLoadDialog(exp: ExpeditionDto) {
        if (exp.status?.uppercase() != "VALIDATED") {
            AlertDialog.Builder(this)
                .setTitle("Chargement impossible")
                .setMessage("L'expédition doit être VALIDATED. Statut actuel : ${exp.status}")
                .setPositiveButton("OK") { _, _ ->
                    if (expeditionMode == MODE_LOAD) finish() else resumeScanning()
                }
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmer chargement — ${exp.expeditionNumber}")
            .setMessage(buildString {
                append("Destination  : ${exp.destination ?: "-"}\n")
                append("Transporteur : ${exp.carrierName ?: "-"}\n")
                append("Chauffeur    : ${exp.driverName ?: "-"}\n")
                append("Camion       : ${exp.truckNumber ?: "-"}\n\n")
                append("Articles :\n${buildLinesText(exp)}\n\n")
                append("⚠️ Cette action déclenche la sortie de stock et génère la preuve de départ.")
            })
            .setPositiveButton("Confirmer chargement") { _, _ -> performShip(exp.id ?: "") }
            .setNegativeButton("Annuler") { _, _ ->
                if (expeditionMode == MODE_LOAD) finish() else resumeScanning()
            }
            .show()
    }

    private fun performShip(expeditionId: String) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            saveOffline(expeditionId, "ship")
            return
        }
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.shipExpedition(
                        expeditionId,
                        ExpeditionActionRequest(comment = "Chargement confirmé via scan mobile")
                    )
                }
                if (response.isSuccessful) {
                    launch(Dispatchers.IO) { OSMApplication.repository.refreshExpedition(expeditionId) }
                    showProofOfDeparture(response.body())
                } else {
                    Toast.makeText(this@ExpeditionScannerActivity, "Erreur chargement : ${response.code()}", Toast.LENGTH_LONG).show()
                    if (expeditionMode == MODE_LOAD) finish() else resumeScanning()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpeditionScannerActivity, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                if (expeditionMode == MODE_LOAD) finish() else resumeScanning()
            }
        }
    }

    private fun showProofOfDeparture(exp: ExpeditionDto?) {
        val now = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val linesText = exp?.lines?.joinToString("\n") { l ->
            "• ${l.articleName ?: "Article"} — ${l.quantity} ${l.unit ?: ""}" +
                    if (!l.lotNumber.isNullOrBlank()) " | Lot: ${l.lotNumber}" else ""
        } ?: "-"

        AlertDialog.Builder(this)
            .setTitle("✅ Chargement confirmé")
            .setMessage(buildString {
                append("━━━━━━━━━━━━━━━━━━━━━━\n")
                append("Expédition  : ${exp?.expeditionNumber ?: "-"}\n")
                append("Destination : ${exp?.destination ?: "-"}\n")
                append("Transporteur: ${exp?.carrierName ?: "-"}\n")
                append("Camion      : ${exp?.truckNumber ?: "-"}\n")
                append("Date        : $now\n")
                append("Statut      : SHIPPED\n")
                append("━━━━━━━━━━━━━━━━━━━━━━\n\n")
                append("Articles :\n$linesText\n\n")
                append("📋 Preuve de sortie enregistrée.")
            })
            .setCancelable(false)
            .setPositiveButton("Terminer") { _, _ -> finish() }
            .show()
    }

    private fun buildLinesText(exp: ExpeditionDto): String =
        exp.lines?.joinToString("\n") { l ->
            "• ${l.articleName ?: "Article"} — ${l.quantity} ${l.unit ?: ""}" +
                    if (!l.lotNumber.isNullOrBlank()) " | Lot: ${l.lotNumber}" else ""
        } ?: "Aucune ligne"

    private fun saveOffline(expeditionId: String, action: String) {
        val body = gson.toJson(ExpeditionActionRequest(comment = "Action mobile hors ligne"))
        OfflineManager.getInstance(this).saveOperation(
            OfflineOperation(
                operationId = UUID.randomUUID().toString(),
                url = "/api/expeditions/$expeditionId/$action",
                method = "POST",
                body = body
            )
        )
        Toast.makeText(this, "Enregistré hors ligne, sera synchronisé plus tard", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val EXTRA_EXPEDITION_MODE = "extra_expedition_mode"
        private const val EXTRA_EXPEDITION_ID = "extra_expedition_id"
        const val MODE_PICK = "PICK"
        const val MODE_LOAD = "LOAD"

        fun newIntent(context: Context) =
            Intent(context, ExpeditionScannerActivity::class.java).apply {
                putExtra(EXTRA_EXPEDITION_MODE, MODE_PICK)
            }

        fun newIntentForLoading(context: Context, expeditionId: String) =
            Intent(context, ExpeditionScannerActivity::class.java).apply {
                putExtra(EXTRA_EXPEDITION_MODE, MODE_LOAD)
                putExtra(EXTRA_EXPEDITION_ID, expeditionId)
            }
    }
}