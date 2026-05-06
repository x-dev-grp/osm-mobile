package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.xdev.osm_mobile.databinding.ActivityOfDetailBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.OfflineOperation
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.models.OrderFabricationDTO
import com.xdev.osm_mobile.myAdapter.ComponentAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.String.format
import java.util.Locale
import java.util.UUID

class OfDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOfDetailBinding
    private var of: OrderFabricationDTO? = null
    private val gson = com.google.gson.GsonBuilder()
        .registerTypeAdapter(com.xdev.osm_mobile.models.ArticleConfig::class.java, com.xdev.osm_mobile.models.ArticleConfigDeserializer())
        .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        val ofId = intent.getStringExtra(EXTRA_OF_ID)
        if (ofId.isNullOrEmpty()) {
            Toast.makeText(this, "ID OF manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadOfDetails(ofId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar?.title = "Détail OF"
    }

    private fun loadOfDetails(ofId: String) {
        val repository = OSMApplication.repository
        
        lifecycleScope.launch {
            if (NetworkUtils.isInternetAvailable(this@OfDetailActivity)) {
                try {
                    repository.refreshOfDetail(ofId)
                } catch (e: Exception) {
                    Log.e("OfDetail", "Erreur refresh network", e)
                }
            }
            val cachedOf = repository.getOfById(ofId)
            if (cachedOf?.fullJson != null) {
                of = gson.fromJson(cachedOf.fullJson, OrderFabricationDTO::class.java)
                displayOfDetails()
                setupActionButtons()
                displayComponents()
            } else {
                Toast.makeText(this@OfDetailActivity, "OF non trouvé en cache", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayOfDetails() {
        val of = this.of ?: return
        with(binding) {
            tvOfNumber.text = of.code ?: "-"
            tvStatus.text = of.statut ?: "-"
            when (of.statut?.uppercase()) {
                "ACTIF", "EN_COURS" -> tvStatus.setBackgroundColor(getColor(R.color.olive_green))
                "CLOTURE", "TERMINE" -> tvStatus.setBackgroundColor(getColor(R.color.colorPrimary))
                else -> tvStatus.setBackgroundColor(getColor(R.color.colorAccent))
            }
            tvSku.text = of.sku?.code ?: of.skuId ?: "-"
            tvLigne.text = of.ligneNom ?: of.ligneId ?: "-"
            tvLotVrac.text = of.lotVracId ?: "Non défini"
            tvTargetQuantity.text = formatQuantity(of.quantiteCible)
            tvProducedQuantity.text = formatQuantity(of.quantiteBonne)
            val remaining = (of.quantiteCible ?: 0.0) - (of.quantiteBonne ?: 0.0)
            tvRemainingQuantity.text = formatQuantity(remaining)
            tvPlannedDate.text = formatDateTime(of.dateDebutPrevue)
            tvPlannedEndDate.text = formatDateTime(of.dateFinPrevue)
            tvActualStartDate.text = formatDateTime(of.dateDebutReelle)
            tvActualEndDate.text = formatDateTime(of.dateFinReelle)
        }
    }
    private fun displayComponents() {
        val of = this.of ?: return
        val recyclerView = binding.rvComponents
        recyclerView.layoutManager = LinearLayoutManager(this)
        val components = of.lignes?.toMutableList() ?: mutableListOf()
        val isActive = of.statut?.uppercase() == "EN_COURS"

        val adapter = ComponentAdapter(
            components = components,
            isEditable = isActive,
            onAdjust = { articleId, quantiteReelle, motif ->
                ajusterConsommation(articleId, quantiteReelle, motif)
            }
        )
        recyclerView.adapter = adapter
    }
    private fun ajusterConsommation(articleId: String, quantiteReelle: Double, motif: String) {
        val ofId = this.of?.id
        if (ofId.isNullOrBlank()) {
            Toast.makeText(this, "ID OF invalide", Toast.LENGTH_SHORT).show()
            return
        }
        val body = mapOf(
            "articleId" to articleId,
            "quantiteReelle" to quantiteReelle,
            "motif" to motif
        )
        val bodyJson = gson.toJson(body)
        if (NetworkUtils.isInternetAvailable(this)) {
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.ajusterConsommation(ofId, body)
                    }
                    if (response.isSuccessful) {
                        Toast.makeText(this@OfDetailActivity, "Ajustement enregistré", Toast.LENGTH_SHORT).show()
                        val adapter = binding.rvComponents.adapter as? ComponentAdapter
                        adapter?.updateComponent(articleId, quantiteReelle, motif)
                    } else {
                        Toast.makeText(this@OfDetailActivity, "Erreur : ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@OfDetailActivity, e.message ?: "Erreur réseau", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val operationId = UUID.randomUUID().toString()
            val url = "/api/ordreConditionement/of/$ofId/ajustements"
            val operation = OfflineOperation(
                operationId = operationId,
                url = url,
                method = "PUT",
                body = bodyJson
            )
            OfflineManager.getInstance(this).saveOperation(operation)
            Toast.makeText(
                this,
                "Ajustement enregistré localement, sera synchronisé plus tard",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun setupActionButtons() {
        val ofId = this.of?.id
        if (ofId.isNullOrBlank()) {
            binding.btnStartProduction.isEnabled = false
            binding.btnPerformQualityControl.isEnabled = false
            return
        }
        binding.btnStartProduction.setOnClickListener {
            startActivity(ProductionEntryActivity.newIntent(this, ofId))
        }
        binding.btnPerformQualityControl.setOnClickListener {
            startActivity(QCActivity.newIntent(this, ofId, of?.code ?: "-"))
        }
        val isActive = of?.statut?.uppercase() == "ACTIF" || of?.statut?.uppercase() == "EN_COURS"
        binding.btnStartProduction.isEnabled = isActive
        binding.btnPerformQualityControl.isEnabled = isActive
    }
    companion object {
        private const val EXTRA_OF_ID = "extra_of_id"
        fun newIntent(context: Context, ofId: String): Intent = Intent(context, OfDetailActivity::class.java).apply {
            putExtra(EXTRA_OF_ID, ofId)
        }
    }
    private fun formatQuantity(value: Double?): String =
        if (value != null) format(Locale.US, "%.2f u", value) else "-"

    private fun formatDateTime(dateString: String?): String =
        dateString?.replace('T', ' ')?.substring(0, 16) ?: "-"
}
