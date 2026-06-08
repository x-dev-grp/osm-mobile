package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xdev.osm_mobile.database.entities.MovementEntity
import com.xdev.osm_mobile.databinding.ActivityStockMovementListBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.ui.adapters.StockMovementAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StockMovementListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockMovementListBinding
    private lateinit var adapter: StockMovementAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var allMovements = listOf<MovementEntity>()
    private var currentFilter = "TOUS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockMovementListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val today = SimpleDateFormat("d MMM yyyy", Locale.FRANCE).format(Date()).uppercase()
        binding.tvDateLabel.text = "AUJOURD'HUI — $today"

        setupRecyclerView()
        setupFilterChips()
        setupBackButton()
        setupScanButton()
        setupSwipeRefresh()
        observeMovements()
        syncIfNeeded()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupScanButton() {
        binding.btnScan.setOnClickListener {
            startActivity(StockMovementScannerActivity.newIntent(this))
        }
    }

    private fun setupRecyclerView() {
        adapter = StockMovementAdapter()
        binding.rvMovements.layoutManager = LinearLayoutManager(this)
        binding.rvMovements.adapter = adapter
    }

    private fun setupFilterChips() {
        val chipBackground = getDrawable(R.drawable.bg_abiooc_button_primary)
        val chipOutline   = getDrawable(R.drawable.bg_abiooc_button_outline)
        val colorSelected = android.graphics.Color.WHITE
        val colorUnselected = getColor(R.color.abiooc_text_dark)

        fun updateChips(active: String) {
            currentFilter = active
            listOf(
                "TOUS"         to binding.chipTous,
                "CONSOMMATION" to binding.chipConsommation,
                "ENTREE"       to binding.chipEntree,
                "AJUSTEMENT"   to binding.chipAjustement
            ).forEach { (key, chip) ->
                val isActive = key == active
                chip.background = if (isActive) chipBackground else chipOutline
                chip.setTextColor(if (isActive) colorSelected else colorUnselected)
            }
            applyFilter()
        }

        binding.chipTous.setOnClickListener         { updateChips("TOUS") }
        binding.chipConsommation.setOnClickListener { updateChips("CONSOMMATION") }
        binding.chipEntree.setOnClickListener       { updateChips("ENTREE") }
        binding.chipAjustement.setOnClickListener   { updateChips("AJUSTEMENT") }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh = binding.swipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                lifecycleScope.launch {
                    try {
                        OSMApplication.repository.refreshAllMovements()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        swipeRefresh.isRefreshing = false
                    }
                }
            } else {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun observeMovements() {
        lifecycleScope.launch {
            OSMApplication.repository.getAllMovements().collectLatest { movements ->
                allMovements = movements
                applyFilter()
            }
        }
    }

    private fun syncIfNeeded() {
        lifecycleScope.launch {
            if (NetworkUtils.isInternetAvailable(this@StockMovementListActivity)) {
                try {
                    OSMApplication.repository.refreshAllMovements()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "TOUS" -> allMovements
            "ENTREE" -> allMovements.filter { it.typeMouvement.uppercase() == "ENTREE" }
            "AJUSTEMENT" -> allMovements.filter { it.typeMouvement.uppercase() == "AJUSTEMENT" }
            "CONSOMMATION" -> allMovements.filter {
                it.typeMouvement.uppercase() == "SORTIE"
            }
            else -> allMovements
        }

        adapter.submitList(filtered)
        updateTotals(filtered)

        val empty = filtered.isEmpty()
        binding.layoutEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvMovements.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun updateTotals(list: List<MovementEntity>) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayMovements = list.filter { it.dateMouvement?.startsWith(todayStr) == true }

        val totalIn  = todayMovements.filter {
            it.typeMouvement.uppercase() == "ENTREE"
        }.sumOf { it.quantity }

        val totalOut = todayMovements.filter {
            it.typeMouvement.uppercase() == "SORTIE"
        }.sumOf { it.quantity }

        binding.tvTotalEntrees.text = "+${formatQty(totalIn)}"
        binding.tvTotalSorties.text = "−${formatQty(totalOut)}"
    }

    private fun formatQty(qty: Int): String {
        return if (qty >= 1000) {
            val k = qty / 1000
            val r = (qty % 1000).toString().padStart(3, '0')
            "$k $r"
        } else qty.toString()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, StockMovementListActivity::class.java)
    }
}