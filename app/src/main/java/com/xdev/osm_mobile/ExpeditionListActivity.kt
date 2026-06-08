package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import com.xdev.osm_mobile.databinding.ActivityExpeditionListBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.models.ExpeditionDto
import com.xdev.osm_mobile.myAdapter.ExpeditionAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
class ExpeditionListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpeditionListBinding
    private lateinit var adapter: ExpeditionAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val gson = Gson()
    private var allExpeditions = listOf<ExpeditionDto>()
    private var currentFilter = "TOUS"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpeditionListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchAndFilters()
        observeExpeditions()
        syncIfNeeded()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Expéditions"
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnScanExpedition.setOnClickListener {
            startActivity(ExpeditionScannerActivity.newIntent(this))
        }
    }

    private fun setupRecyclerView() {
        adapter = ExpeditionAdapter { expedition ->
            startActivity(ExpeditionDetailActivity.newIntent(this, expedition.id ?: ""))
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh = binding.swipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                lifecycleScope.launch {
                    try {
                        OSMApplication.repository.refreshAllExpeditions()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@ExpeditionListActivity, "Erreur de synchronisation", Toast.LENGTH_SHORT).show()
                    } finally {
                        swipeRefresh.isRefreshing = false
                    }
                }
            } else {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Pas de connexion internet", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.lowercase() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        val chipBackground = getDrawable(R.drawable.bg_abiooc_button_primary)
        val chipOutline = getDrawable(R.drawable.bg_abiooc_button_outline)
        val colorSelected = android.graphics.Color.WHITE
        val colorUnselected = getColor(R.color.abiooc_text_dark)

        fun updateChips(active: String) {
            currentFilter = active
            val chips = mapOf(
                "TOUS" to binding.chipTous,
                "DRAFT" to binding.chipDraft,
                "READY" to binding.chipReady,
                "VALIDATED" to binding.chipValidated,
                "DELIVERED" to binding.chipShipped
            )
            for ((key, chip) in chips) {
                val isActive = key == active
                chip.background = if (isActive) chipBackground else chipOutline
                chip.setTextColor(if (isActive) colorSelected else colorUnselected)
            }
            applyFilters()
        }

        binding.chipTous.setOnClickListener { updateChips("TOUS") }
        binding.chipDraft.setOnClickListener { updateChips("DRAFT") }
        binding.chipReady.setOnClickListener { updateChips("READY") }
        binding.chipValidated.setOnClickListener { updateChips("VALIDATED") }
        binding.chipShipped.setOnClickListener { updateChips("DELIVERED") }
    }

    private fun observeExpeditions() {
        lifecycleScope.launch {
            OSMApplication.repository.allExpeditions.collectLatest { entities ->
                val dtos = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    entities.mapNotNull { entity ->
                        try {
                            gson.fromJson(entity.fullJson ?: "{}", ExpeditionDto::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.plannedShipDate }
                }
                allExpeditions = dtos
                applyFilters()
            }
        }
    }

    private fun syncIfNeeded() {
        lifecycleScope.launch {
            if (NetworkUtils.isInternetAvailable(this@ExpeditionListActivity)) {
                try {
                    OSMApplication.repository.refreshAllExpeditions()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun applyFilters() {
        var filtered = allExpeditions
        if (currentFilter != "TOUS") {
            filtered = filtered.filter { it.status?.uppercase() == currentFilter }
        }
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.expeditionNumber?.lowercase()?.contains(searchQuery) == true ||
                        it.projetCode?.lowercase()?.contains(searchQuery) == true ||
                        it.destination?.lowercase()?.contains(searchQuery) == true
            }
        }
        adapter.submitList(filtered)
        val isEmpty = filtered.isEmpty()
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, ExpeditionListActivity::class.java)
    }
}