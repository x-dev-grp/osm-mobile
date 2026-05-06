package com.xdev.osm_mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.xdev.osm_mobile.myAdapter.SyncQueueAdapter
import com.xdev.osm_mobile.myAdapter.SyncItem
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.SyncStatus
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.Toast

class SyncQueueActivity : AppCompatActivity() {
    private lateinit var offlineManager: OfflineManager
    private lateinit var adapter: SyncQueueAdapter
    private val syncItems = mutableListOf<SyncItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_queue)
        
        supportActionBar?.title = "File de synchronisation"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        offlineManager = OfflineManager.getInstance(this)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = SyncQueueAdapter(syncItems) { item ->
            when (item) {
                is SyncItem.Scan -> {
                    offlineManager.updateScanStatus(item.id, SyncStatus.PENDING)
                    offlineManager.syncPendingScans { _, _ -> refreshList() }
                }
                is SyncItem.Operation -> {
                    offlineManager.updateOperationStatus(item.id, SyncStatus.PENDING)
                    offlineManager.syncPendingOperations { _, _ -> refreshList() }
                }
            }
            refreshList()
        }
        recyclerView.adapter = adapter
        
        findViewById<Button>(R.id.btnClearSynced).setOnClickListener {
            offlineManager.clearScansByStatus(SyncStatus.SYNCED)
            offlineManager.clearOperationsByStatus(SyncStatus.SYNCED)
            refreshList()
            Toast.makeText(this, "File nettoyée", Toast.LENGTH_SHORT).show()
        }
        
        refreshList()
    }

    private fun refreshList() {
        syncItems.clear()
        val scans = offlineManager.getAllScansWithStatus().map { SyncItem.Scan(it) }
        syncItems.addAll(scans)
        val operations = offlineManager.getAllOperationsWithStatus().map { SyncItem.Operation(it) }
        syncItems.addAll(operations)
        syncItems.sortByDescending { it.timestamp }
        
        adapter.notifyDataSetChanged()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
