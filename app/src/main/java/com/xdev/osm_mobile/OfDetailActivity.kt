package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xdev.osm_mobile.databinding.ActivityOfDetailBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.models.OrderFabricationDTO
import com.xdev.osm_mobile.myAdapter.ComponentAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class OfDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOfDetailBinding
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var of: OrderFabricationDTO? = null
    private var ofId: String = ""

    private val gson = com.google.gson.GsonBuilder()
        .registerTypeAdapter(com.xdev.osm_mobile.models.ArticleConfig::class.java, com.xdev.osm_mobile.models.ArticleConfigDeserializer())
        .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupHeader()
        setupSwipeRefresh()
        ofId = intent.getStringExtra(EXTRA_OF_ID) ?: run {
            Toast.makeText(this, "ID OF manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadOfDetails()
    }
    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    private fun setupSwipeRefresh() {
        swipeRefresh = binding.swipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                lifecycleScope.launch {
                    refreshFromBackend()
                    swipeRefresh.isRefreshing = false
                }
            } else {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Pas de connexion internet", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private suspend fun refreshFromBackend() {
        try {
            withContext(Dispatchers.IO) {
                OSMApplication.repository.refreshOfDetail(ofId)
            }
            loadOfDetails(forceRefresh = true)
            Toast.makeText(this, "Synchronisation réussie", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("OfDetail", "Erreur synchronisation", e)
            Toast.makeText(this, "Erreur de synchronisation", Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadOfDetails(forceRefresh: Boolean = false) {
        lifecycleScope.launch {
            if (forceRefresh) {
                withContext(Dispatchers.IO) { kotlinx.coroutines.delay(500) }
            }
            val cachedOf = withContext(Dispatchers.IO) {
                OSMApplication.repository.getOfById(ofId)
            }
            if (cachedOf?.fullJson != null) {
                of = gson.fromJson(cachedOf.fullJson, OrderFabricationDTO::class.java)
                displayOfDetails()
                displayQualityStatus()
                setupActionButtons()
                displayComponents()
                loadQCStats(ofId)
            } else {
                if (NetworkUtils.isInternetAvailable(this@OfDetailActivity)) {
                    refreshFromBackend()
                } else {
                    Toast.makeText(
                        this@OfDetailActivity,
                        "Aucune donnée disponible pour cet OF",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun displayQualityStatus() {
        val qualityStatus = of?.qualityStatus ?: return
        binding.tvQualityStatus.visibility = View.VISIBLE
        if (qualityStatus.equals("BLOCKED", ignoreCase = true)) {
            binding.tvQualityStatus.text = "⚠️ BLOQUÉ PAR LA QUALITÉ"
            binding.tvQualityStatus.setBackgroundColor(android.graphics.Color.parseColor("#C62828"))
            binding.tvQualityStatus.setTextColor(android.graphics.Color.WHITE)
            Toast.makeText(this, "OF BLOQUÉ PAR LA QUALITÉ", Toast.LENGTH_LONG).show()
        } else {
            binding.tvQualityStatus.text = "✓ QUALITÉ : LIBRE"
            binding.tvQualityStatus.setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
            binding.tvQualityStatus.setTextColor(android.graphics.Color.WHITE)
        }
    }

    private fun displayOfDetails() {
        val of = this.of ?: return
        with(binding) {
            tvOfNumber.text = of.code ?: "-"
            tvStatus.text = of.statut?.uppercase() ?: "-"

            val statusColor = when (of.statut?.uppercase()) {
                "ACTIF", "EN_COURS" -> "#2E7D32"
                "CLOTURE", "TERMINE" -> "#1976D2"
                "STOP", "ANNULE" -> "#C62828"
                else -> "#E65100"
            }
            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(statusColor))
            tvSku.text = of.skuCode ?: of.skuId ?: "-"
            tvProduct.text = of.skuCode ?: "-"
            tvLigne.text = of.ligneNom ?: of.ligneId ?: "-"
            tvLotVrac.text = of.lotVracNom ?: "Non défini"
            val target = of.quantiteCible ?: 0.0
            val produced = of.quantiteBonne ?: 0.0
            val nc = of.quantiteNC ?: 0.0
            val remaining = (target - produced).coerceAtLeast(0.0)
            tvTargetQty.text = formatQuantitySimple(target)
            tvProducedQty.text = formatQuantitySimple(produced)
            tvRemainingQty.text = formatQuantitySimple(remaining)
            val progress = if (target > 0) ((produced / target) * 100).toInt() else 0
            tvProgressPercent.text = "$progress%"
            pbAvancement.progress = progress
            tvNcQuantity.text = "${formatQuantitySimple(nc)} u"
            val rate = if (produced + nc > 0) (produced / (produced + nc)) * 100 else 100.0
            tvConformityRate.text = String.format(Locale.US, "%.1f%%", rate)
            tvPlannedDate.text = formatDateTime(of.dateDebutPrevue)
            tvPlannedEndDate.text = formatDateTime(of.dateFinPrevue)
            tvActualStartDate.text = formatDateTime(of.dateDebutReelle)
            tvActualEndDate.text = if (of.dateFinReelle != null) formatDateTime(of.dateFinReelle) else "— en cours"
            tvElapsedTime.text = formatElapsedTime(of.dureeReelle, of.dateDebutReelle, of.dateFinReelle)
        }
    }
    private fun formatElapsedTime(dureeReelleMillis: Long?, debutReelle: String?, finReelle: String?): String {
        if (dureeReelleMillis != null && dureeReelleMillis > 0) {
            return millisToElapsedString(dureeReelleMillis)
        }
        val start = debutReelle?.let { parseDateTime(it) } ?: return "—"
        val end = finReelle?.let { parseDateTime(it) } ?: Date() // en cours → maintenant
        val diffMillis = end.time - start.time
        if (diffMillis <= 0) return "—"
        return millisToElapsedString(diffMillis)
    }

    private fun millisToElapsedString(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            minutes > 0 -> "${minutes}min"
            else -> "< 1min"
        }
    }

    private fun parseDateTime(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            formatter.parse(dateString)
        } catch (e: Exception) {
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                formatter.parse(dateString)
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun loadQCStats(ofId: String) {
        lifecycleScope.launch {
            if (NetworkUtils.isInternetAvailable(this@OfDetailActivity)) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        com.xdev.osm_mobile.network.RetrofitClient.instance.getQCHistory(ofId)
                    }
                    if (response.isSuccessful && response.body() != null) {
                        val history = response.body()!!.data ?: emptyList()
                        val ok = history.count { it.statut?.uppercase() == "OK" }
                        val nok = history.count { it.statut?.uppercase() == "NOK" }
                        binding.tvQcOk.text = ok.toString()
                        binding.tvQcNok.text = nok.toString()
                    }
                } catch (e: Exception) {
                    Log.e("OfDetail", "Error loading QC stats", e)
                }
            }
        }
    }

    private fun displayComponents() {
        val of = this.of ?: return
        val recyclerView = binding.rvComponents
        recyclerView.layoutManager = LinearLayoutManager(this)
        val components = of.lignes?.toMutableList() ?: mutableListOf()
        val adapter = ComponentAdapter(components = components, isEditable = false)
        recyclerView.adapter = adapter
    }

    private fun setupActionButtons() {
        val ofId = this.of?.id
        if (ofId.isNullOrBlank()) return

        val isBlocked = of?.qualityStatus.equals("BLOCKED", ignoreCase = true)
        binding.btnStartProduction.isEnabled = !isBlocked
        binding.btnStartProduction.setOnClickListener {
            startActivity(ProductionEntryActivity.newIntent(this, ofId))
        }
        binding.btnPerformQualityControl.setOnClickListener {
            startActivity(QCActivity.newIntent(this, ofId, of?.code ?: "-"))
        }
    }

    private fun formatQuantitySimple(value: Double?): String =
        if (value != null) String.format(Locale.US, "%,.0f", value).replace(',', ' ') else "0"

    private fun formatDateTime(dateString: String?): String {
        if (dateString == null) return "—"
        return try {
            val parts = dateString.split("T")
            val date = parts[0]
            val time = parts[1].substring(0, 5)
            "$date — $time"
        } catch (e: Exception) {
            dateString
        }
    }

    companion object {
        private const val EXTRA_OF_ID = "extra_of_id"
        fun newIntent(context: Context, ofId: String): Intent = Intent(context, OfDetailActivity::class.java).apply {
            putExtra(EXTRA_OF_ID, ofId)
        }
    }
}