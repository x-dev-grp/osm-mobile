package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xdev.osm_mobile.databinding.ActivityQchistoryDetailBinding
import com.xdev.osm_mobile.models.QCResultDTO
import com.xdev.osm_mobile.myAdapter.QCHistoryAdapter
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QCHistoryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQchistoryDetailBinding
    private lateinit var adapter: QCHistoryAdapter
    private var fullHistory: List<QCResultDTO> = emptyList()
    private var currentOfId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQchistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSwipeRefresh()

        val ofId = intent.getStringExtra(EXTRA_OF_ID)
        val ofLabel = intent.getStringExtra(EXTRA_OF_LABEL) ?: "OF"

        if (ofId.isNullOrEmpty()) {
            Toast.makeText(this, "ID OF manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentOfId = ofId
        binding.tvOfDescHeader.text = ofLabel

        binding.btnNewControl.setOnClickListener {
            startActivity(QCActivity.newIntent(this, ofId, ofLabel))
        }

        setupFilterChips()
        setupDateHeader()
        loadHistory(ofId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadHistory(currentOfId)
        }
        // Couleurs du loader (optionnel)
        binding.swipeRefreshLayout.setColorSchemeColors(
            getColor(android.R.color.holo_blue_dark),
            getColor(android.R.color.holo_green_dark),
            getColor(android.R.color.holo_orange_dark)
        )
    }

    private fun setupDateHeader() {
        val sdf = java.text.SimpleDateFormat("dd MMMM", java.util.Locale.FRANCE)
        val today = sdf.format(java.util.Date()).uppercase()
        binding.tvDateSection.text = "AUJOURD'HUI - $today"
    }

    private fun setupFilterChips() {
        val chips = listOf(binding.chipTous, binding.chipConformes, binding.chipNok)
        val onChipClick = android.view.View.OnClickListener { view ->
            for (chip in chips) {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected_outline)
                chip.setTextColor(android.graphics.Color.parseColor("#4A4A4A"))
            }
            val selected = view as android.widget.TextView
            selected.setBackgroundResource(R.drawable.bg_chip_selected_outline)
            selected.setTextColor(android.graphics.Color.parseColor("#1976D2"))

            val filtered = when (selected.id) {
                R.id.chipConformes -> fullHistory.filter { it.statut == "OK" || it.statut?.equals("CONFORME", true) == true }
                R.id.chipNok -> fullHistory.filter { it.statut?.startsWith("NOK", true) == true }
                else -> fullHistory
            }
            updateRecyclerView(filtered)
        }
        for (chip in chips) {
            chip.setOnClickListener(onChipClick)
        }
    }

    private fun loadHistory(ofId: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getQCHistory(ofId)
                }
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    val history: List<QCResultDTO> = apiResponse.data ?: emptyList()
                    if (history.isEmpty()) {
                        Toast.makeText(this@QCHistoryDetailActivity, "Aucun contrôle QC enregistré", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        fullHistory = history
                        updateStats(history)
                        updateRecyclerView(history)
                    }
                } else {
                    Toast.makeText(this@QCHistoryDetailActivity, "Erreur chargement historique", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@QCHistoryDetailActivity, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateStats(history: List<QCResultDTO>) {
        val total = history.size
        val nokList = history.filter { it.statut?.startsWith("NOK", ignoreCase = true) == true }
        val bloquant = nokList.count {
            it.statut?.contains("BLOQUANT", ignoreCase = true) == true || it.statut == "NOK"
        }
        binding.tvTotalControls.text = total.toString()
        binding.tvNokBloquant.text = bloquant.toString()
        binding.tvControlsCount.text = "$total contrôle${if (total > 1) "s" else ""}"
    }

    private fun updateRecyclerView(history: List<QCResultDTO>) {
        adapter = QCHistoryAdapter(history)
        binding.rvQCHistory.layoutManager = LinearLayoutManager(this)
        binding.rvQCHistory.adapter = adapter
    }

    companion object {
        private const val EXTRA_OF_ID = "extra_of_id"
        private const val EXTRA_OF_LABEL = "extra_of_label"

        fun newIntent(context: Context, ofId: String, ofLabel: String): Intent =
            Intent(context, QCHistoryDetailActivity::class.java).apply {
                putExtra(EXTRA_OF_ID, ofId)
                putExtra(EXTRA_OF_LABEL, ofLabel)
            }
    }
}