package com.xdev.osm_mobile

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.xdev.osm_mobile.databinding.ActivitySyncQueueBinding
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.SyncStatus
import com.xdev.osm_mobile.myAdapter.SyncItem
import com.xdev.osm_mobile.myAdapter.SyncQueueAdapter

class SyncQueueActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySyncQueueBinding
    private lateinit var offlineManager: OfflineManager
    private lateinit var adapter: SyncQueueAdapter
    private val allOperations = mutableListOf<SyncItem.Operation>()
    private var currentFilter: SyncStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySyncQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        offlineManager = OfflineManager.getInstance(this)
        setupRecyclerView()
        setupButtons()
        setupFilters()
        applyWindowInsets()
        refreshList()
    }
    private fun setupRecyclerView() {
        adapter = SyncQueueAdapter(items = emptyList(), onRetry = { item -> retryItem(item) }, onDelete = { item -> deleteItem(item)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SyncQueueActivity)
            adapter = this@SyncQueueActivity.adapter
        }
    }
    private fun setupButtons() {
        binding.btnClearSynced.setOnClickListener {
            offlineManager.clearOperationsByStatus(SyncStatus.SYNCED)
            refreshList()
            Toast.makeText(this, "Opérations synchronisées supprimées", Toast.LENGTH_SHORT).show()
        }
        binding.btnClearAll.setOnClickListener {
            offlineManager.clearAllOperations()
            refreshList()
            Toast.makeText(this, "Toute la file a été supprimée", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setupFilters() {
        binding.btnFilterAll.setOnClickListener { setFilter(null) }
        binding.btnFilterPending.setOnClickListener { setFilter(SyncStatus.PENDING) }
        binding.btnFilterSynced.setOnClickListener { setFilter(SyncStatus.SYNCED) }
        binding.btnFilterError.setOnClickListener { setFilter(SyncStatus.ERROR) }
        updateFilterUI()
    }
    private fun setFilter(status: SyncStatus?) {
        currentFilter = status
        updateFilterUI()
        applyCurrentFilter()
    }
    private fun applyCurrentFilter() {
        val filtered = if (currentFilter == null) {
            allOperations
        } else {
            allOperations.filter { it.status == currentFilter }
        }
        adapter.updateData(filtered)
    }
    private fun retryItem(item: SyncItem) {
        when (item) {
            is SyncItem.Operation -> {
                offlineManager.updateOperationStatus(item.id, SyncStatus.PENDING)
                offlineManager.syncPendingOperations { _, _ ->
                    refreshList()
                }
            }

        }
    }

    private fun deleteItem(item: SyncItem) {
        when (item) {
            is SyncItem.Operation -> offlineManager.deleteOperation(item.id)
        }
        refreshList()
    }
    private fun refreshList() {
        allOperations.clear()
        val operations = offlineManager.getAllOperationsWithStatus().map { SyncItem.Operation(it) }
        allOperations.addAll(operations)
        allOperations.sortByDescending { it.timestamp }

        updateCounts()
        applyCurrentFilter()
    }

    private fun updateCounts() {
        val pending = allOperations.count { it.status == SyncStatus.PENDING }
        val synced = allOperations.count { it.status == SyncStatus.SYNCED }
        val error = allOperations.count { it.status == SyncStatus.ERROR }

        binding.tvCountPending.text = pending.toString()
        binding.tvCountSynced.text = synced.toString()
        binding.tvCountError.text = error.toString()

        binding.btnFilterAll.text = "Tous (${allOperations.size})"
    }


//gère espace système Android
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom
            )
            insets
        }
    }
    private fun updateFilterUI() {
        val activeColor = ContextCompat.getColor(this, R.color.abiooc_primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.gray)
        val activeBg = ContextCompat.getColor(this, R.color.sync_success_bg)
        val inactiveBg = ContextCompat.getColor(this, android.R.color.transparent)

        binding.btnFilterAll.apply {
            strokeColor = ColorStateList.valueOf(if (currentFilter == null) activeColor else inactiveColor)
            backgroundTintList = ColorStateList.valueOf(if (currentFilter == null) activeBg else inactiveBg)
        }
        binding.btnFilterPending.apply {
            strokeColor = ColorStateList.valueOf(if (currentFilter == SyncStatus.PENDING) activeColor else inactiveColor)
            backgroundTintList = ColorStateList.valueOf(if (currentFilter == SyncStatus.PENDING) activeBg else inactiveBg)
        }
        binding.btnFilterSynced.apply {
            strokeColor = ColorStateList.valueOf(if (currentFilter == SyncStatus.SYNCED) activeColor else inactiveColor)
            backgroundTintList = ColorStateList.valueOf(if (currentFilter == SyncStatus.SYNCED) activeBg else inactiveBg)
        }
        binding.btnFilterError.apply {
            strokeColor = ColorStateList.valueOf(if (currentFilter == SyncStatus.ERROR) activeColor else inactiveColor)
            backgroundTintList = ColorStateList.valueOf(if (currentFilter == SyncStatus.ERROR) activeBg else inactiveBg)
        }
    }
}