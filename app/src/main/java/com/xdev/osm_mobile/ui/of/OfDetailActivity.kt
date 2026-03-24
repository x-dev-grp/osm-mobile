package com.xdev.osm_mobile.ui.of

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.xdev.osm_mobile.databinding.ActivityOfDetailBinding
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.network.models.OrderFabricationDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OfDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfDetailBinding
    private lateinit var of: OrderFabricationDTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ofNumber = intent.getStringExtra(EXTRA_OF_NUMBER)
        if (ofNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Numero OF manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        loadOfDetails(ofNumber)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar?.title = "Detail OF"
    }

    private fun loadOfDetails(ofNumber: String) {
        lifecycleScope.launch {
            try {
                val found = withContext(Dispatchers.IO) {
                    val response = RetrofitClient.instance.getOfs()
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Erreur chargement OF")
                    }

                    response.body().orEmpty().find { it.code == ofNumber }
                        ?: throw IllegalStateException("OF non trouve")
                }

                of = found
                displayOfDetails()
                setupActionButtons()
            } catch (e: Exception) {
                Toast.makeText(this@OfDetailActivity, e.message ?: "Erreur", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun displayOfDetails() {
        with(binding) {
            tvOfNumber.text = of.code ?: "-"
            tvStatus.text = of.statut ?: "-"
            tvStatus.setBackgroundColor(getStatusColor(of.statut))

            tvSku.text = of.sku?.code ?: of.skuCode ?: "-"
            tvProductName.text =
                of.sku?.category ?: of.ligneConditionnement?.nom ?: of.ligneNom ?: "-"
            tvLotVrac.text = of.lotVracId ?: "Non defini"

            tvTargetQuantity.text = formatQuantity(of.quantiteCible)
            tvProducedQuantity.text = formatQuantity(of.quantiteBonne)
            val remaining = (of.quantiteCible ?: 0.0) - (of.quantiteBonne ?: 0.0)
            tvRemainingQuantity.text = formatQuantity(remaining)

            tvPlannedDate.text = formatDate(of.dateDebutPrevue)
            tvActualStartDate.text = formatDate(of.dateDebutReelle)
            tvActualEndDate.text = formatDate(of.dateFinReelle)
        }
    }

    private fun getStatusColor(status: String?): Int {
        val colorRes = when (status?.uppercase()) {
            "PLANIFIE" -> android.R.color.holo_orange_light
            "ACTIF" -> android.R.color.holo_blue_light
            "TERMINE" -> android.R.color.holo_green_light
            "ANNULE" -> android.R.color.holo_red_light
            else -> android.R.color.darker_gray
        }
        return ContextCompat.getColor(this, colorRes)
    }

    private fun formatQuantity(value: Double?): String =
        if (value != null) String.format("%.2f u", value) else "-"

    private fun formatDate(dateString: String?): String =
        dateString?.take(10) ?: "-"

    private fun setupActionButtons() {
        binding.btnStartProduction.setOnClickListener {
            Toast.makeText(this, "Demarrer production (a implementer)", Toast.LENGTH_SHORT).show()
        }

        binding.btnPerformQualityControl.setOnClickListener {
            Toast.makeText(this, "Controle qualite (a implementer)", Toast.LENGTH_SHORT).show()
        }

        val isActive = of.statut?.uppercase() == "ACTIF"
        binding.btnStartProduction.isEnabled = isActive
        binding.btnPerformQualityControl.isEnabled = isActive
    }

    companion object {
        private const val EXTRA_OF_NUMBER = "extra_of_number"

        fun newIntent(context: Context, ofNumber: String): Intent =
            Intent(context, OfDetailActivity::class.java).apply {
                putExtra(EXTRA_OF_NUMBER, ofNumber)
            }
    }
}
