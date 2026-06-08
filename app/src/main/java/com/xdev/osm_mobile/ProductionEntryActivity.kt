package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.xdev.osm_mobile.databinding.ActivityProductionEntryBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.*
import com.xdev.osm_mobile.myAdapter.ComponentAdapter
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class ProductionEntryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductionEntryBinding
    private var of: OrderFabricationDTO? = null
    private var components = mutableListOf<LigneOFDto>()
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ArticleConfig::class.java, ArticleConfigDeserializer())
        .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductionEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        val ofId = intent.getStringExtra(EXTRA_OF_ID)
        if (ofId.isNullOrEmpty()) {
            Toast.makeText(this, "ID OF manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadOfDetails(ofId)
        setupListeners()
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadOfDetails(ofId: String) {
        lifecycleScope.launch {
            val cached = OSMApplication.repository.getOfById(ofId)
            if (cached?.fullJson != null) {
                of = gson.fromJson(cached.fullJson, OrderFabricationDTO::class.java)
                components = of?.lignes?.toMutableList() ?: mutableListOf()
                displayOfInfo()
                setupComponentsList()
            } else {
                if (NetworkUtils.isInternetAvailable(this@ProductionEntryActivity)) {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.instance.getOfById(ofId)
                        }
                        if (response.isSuccessful && response.body() != null) {
                            of = response.body()!!
                            components = of?.lignes?.toMutableList() ?: mutableListOf()
                            displayOfInfo()
                            setupComponentsList()
                            OSMApplication.repository.refreshOfDetail(ofId)
                        } else {
                            Toast.makeText(this@ProductionEntryActivity, "OF non trouvé", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e("ProductionEntry", "Error loading OF", e)
                        Toast.makeText(this@ProductionEntryActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@ProductionEntryActivity, "Données non disponibles hors ligne", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun displayOfInfo() {
        val of = this.of ?: return
        with(binding) {
            tvOfNumber.text = of.code ?: "-"
            tvProduct.text = of.sku?.code ?: of.skuId ?: "-"
            tvTargetQuantity.text = formatQuantity(of.quantiteCible)
            tvProducedQuantity.text = formatQuantity(of.quantiteBonne)
            val remaining = (of.quantiteCible ?: 0.0) - (of.quantiteBonne ?: 0.0)
            tvRemainingQuantity.text = formatQuantity(remaining.coerceAtLeast(0.0))

            if (!of.motifNC.isNullOrEmpty()) {
                etMotifNC.setText(of.motifNC)
            }
        }
    }

    private fun setupComponentsList() {
        binding.rvComponents.layoutManager = LinearLayoutManager(this)
        val adapter = ComponentAdapter(
            components = components,
            isEditable = true,
            onAdjust = { articleId, quantiteReelle, motif ->
                ajusterConsommation(articleId, quantiteReelle, motif)
            }
        )
        binding.rvComponents.adapter = adapter
    }
    private fun ajusterConsommation(articleId: String, quantiteReelle: Double, motif: String) {
        val ofId = of?.id ?: return
        val index = components.indexOfFirst { it.articleId == articleId }
        if (index >= 0) {
            components[index] = components[index].copy(
                quantiteReelle = quantiteReelle,
                motifAjustement = motif
            )
            (binding.rvComponents.adapter as? ComponentAdapter)?.notifyItemChanged(index)
        }
        lifecycleScope.launch {
            OSMApplication.repository.updateLocalConsumption(ofId, articleId, quantiteReelle, motif)
        }
        val body = mapOf(
            "articleId" to articleId,
            "quantiteReelle" to quantiteReelle,
            "motif" to motif
        )
        val bodyJson = gson.toJson(body)
        val url = "/api/ordreConditionement/of/$ofId/ajustements"

        if (NetworkUtils.isInternetAvailable(this)) {
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.ajusterConsommation(ofId, body)
                    }
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProductionEntryActivity, "Ajustement synchronisé", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProductionEntryActivity, "Erreur serveur, modification locale", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ProductionEntryActivity, "Erreur réseau, modification locale", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val operationId = UUID.randomUUID().toString()
            val operation = OfflineOperation(
                operationId = operationId,
                url = url,
                method = "PUT",
                body = bodyJson
            )
            OfflineManager.getInstance(this).saveOperation(operation)
            Toast.makeText(this, "Ajustement enregistré localement", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setupListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalInfo()
                val nc = binding.etNcQuantity.text.toString().toDoubleOrNull() ?: 0.0
                binding.layoutMotifNC.visibility = if (nc > 0) View.VISIBLE else View.GONE
            }
        }
        binding.etGoodQuantity.addTextChangedListener(textWatcher)
        binding.etNcQuantity.addTextChangedListener(textWatcher)

        binding.btnSaveProduction.setOnClickListener {
            saveProduction()
        }
    }

    private fun updateTotalInfo() {
        val good = binding.etGoodQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val nc = binding.etNcQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val total = good + nc
        binding.tvTotalInfo.text = String.format(Locale.US, "Total : %.2f u", total)
    }

    private fun saveProduction() {
        val ofId = of?.id ?: return
        val goodQuantity = binding.etGoodQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val ncQuantity = binding.etNcQuantity.text.toString().toDoubleOrNull() ?: 0.0

        if (goodQuantity <= 0 && ncQuantity <= 0) {
            Toast.makeText(this, "Veuillez saisir une quantité", Toast.LENGTH_SHORT).show()
            return
        }
        val motifNC = if (ncQuantity > 0) {
            val motif = binding.etMotifNC.text.toString().trim()
            if (motif.isEmpty()) {
                binding.tvMotifError.visibility = View.VISIBLE
                return
            }
            motif
        } else null

        val request = SaisieProductionRequest(
            quantiteBonne = goodQuantity,
            quantiteNC = ncQuantity,
            motifNC = motifNC,
            lignes = components
        )

        val bodyJson = gson.toJson(request)
        val operationId = UUID.randomUUID().toString()
        val url = "/api/ordreConditionement/of/$ofId/production"

        if (NetworkUtils.isInternetAvailable(this)) {
            lifecycleScope.launch {
                try {
                    val syncRequest = SyncRequest(
                        operationId = operationId,
                        url = url,
                        method = "PUT",
                        body = bodyJson
                    )
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.syncOperation(syncRequest)
                    }
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProductionEntryActivity, "Production enregistrée", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@ProductionEntryActivity, "Erreur serveur (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ProductionEntryActivity, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val operation = OfflineOperation(
                operationId = operationId,
                url = url,
                method = "PUT",
                body = bodyJson
            )
            OfflineManager.getInstance(this).saveOperation(operation)
            Toast.makeText(this, "Production enregistrée localement (mode hors-ligne)", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun formatQuantity(value: Double?): String =
        if (value != null) String.format(Locale.US, "%,.0f u", value).replace(',', ' ') else "-"

    companion object {
        private const val EXTRA_OF_ID = "extra_of_id"
        fun newIntent(context: Context, ofId: String): Intent =
            Intent(context, ProductionEntryActivity::class.java).apply {
                putExtra(EXTRA_OF_ID, ofId)
            }
    }
}