package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.text.Editable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.xdev.osm_mobile.databinding.ActivityProductionEntryBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.OfflineOperation
import com.xdev.osm_mobile.models.OrderFabricationDTO
import com.xdev.osm_mobile.models.SaisieProductionRequest
import com.xdev.osm_mobile.models.SyncRequest
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class ProductionEntryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductionEntryBinding
    private lateinit var of: OrderFabricationDTO
    private val gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductionEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        val ofId = intent.getStringExtra(EXTRA_OF_ID)
        if (ofId.isNullOrEmpty()) {
            Toast.makeText(this, "ID OF manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadOfDetails(ofId)
        setupListeners()
    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar?.title = "Saisie production"
    }
    private fun loadOfDetails(ofId: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getOfById(ofId)
                }
                if (response.isSuccessful && response.body() != null) {
                    of = response.body()!!
                    displayOfInfo()
                } else {
                    Toast.makeText(this@ProductionEntryActivity, "OF non trouvé", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProductionEntryActivity, e.message ?: "Erreur réseau", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun displayOfInfo() {
        with(binding) {
            tvOfNumber.text = of.code ?: "-"
            tvProduct.text = of.sku?.code ?: of.skuId ?: "-"
            tvTargetQuantity.text = formatQuantity(of.quantiteCible)
            tvProducedQuantity.text = formatQuantity(of.quantiteBonne)
            val remaining = (of.quantiteCible ?: 0.0) - (of.quantiteBonne ?: 0.0)
            tvRemainingQuantity.text = formatQuantity(remaining)
            if (!of.motifNC.isNullOrEmpty()) {
                etMotifNC.setText(of.motifNC)
                etMotifNC.isEnabled = false
                tvMotifInfo.visibility = android.view.View.VISIBLE
                tvMotifInfo.text = "Motif précédent: ${of.motifNC}"
            }
        }
    }
    private fun setupListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalInfo()
                val nc = binding.etNcQuantity.text.toString().toDoubleOrNull() ?: 0.0
                if (nc > 0) {
                    binding.layoutMotifNC.visibility = android.view.View.VISIBLE
                } else {
                    binding.layoutMotifNC.visibility = android.view.View.GONE
                    binding.tvMotifError.visibility = android.view.View.GONE
                }
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
        binding.tvTotalInfo.text = String.format(Locale.US, "Total : %.2f", total)
    }
    private fun saveProduction() {
        val goodQuantity = binding.etGoodQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val ncQuantity = binding.etNcQuantity.text.toString().toDoubleOrNull() ?: 0.0

        if (goodQuantity <= 0 && ncQuantity <= 0) {
            Toast.makeText(this, "Veuillez saisir une quantité positive", Toast.LENGTH_SHORT).show()
            return
        }
        val motifNC = if (ncQuantity > 0) {
            val motif = binding.etMotifNC.text.toString().trim()
            if (motif.isEmpty()) {
                Toast.makeText(this, "Veuillez saisir le motif des non-conformités", Toast.LENGTH_SHORT).show()
                return
            }
            motif
        } else {
            null
        }
        val ofId = of.id ?: return
        val productionRequest = SaisieProductionRequest(quantiteBonne = goodQuantity, quantiteNC = ncQuantity, motifNC = motifNC)
        val bodyJson = gson.toJson(productionRequest)
        val operationId = UUID.randomUUID().toString()
        val syncRequest = SyncRequest(
            operationId = operationId,
            url = "/api/ordreConditionement/of/$ofId/production",
            method = "PUT",
            body = bodyJson
        )
        if (NetworkUtils.isInternetAvailable(this)) {
            lifecycleScope.launch {
                try {
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
                    Toast.makeText(this@ProductionEntryActivity, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val operation = OfflineOperation(
                operationId = operationId,
                url = "/api/ordreConditionement/of/$ofId/production",
                method = "PUT",
                body = bodyJson
            )
            OfflineManager.getInstance(this).saveOperation(operation)
            Toast.makeText(this, "Production enregistrée localement, sera synchronisée plus tard", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
    private fun formatQuantity(value: Double?): String =
        if (value != null) String.format(Locale.US, "%.2f u", value) else "-"
    companion object {
        private const val EXTRA_OF_ID = "extra_of_id"

        fun newIntent(context: Context, ofId: String): Intent =
            Intent(context, ProductionEntryActivity::class.java).apply {
                putExtra(EXTRA_OF_ID, ofId)
            }
    }
}
