package com.xdev.osm_mobile.ui.sync

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.utils.horsligne.OfflineManager
import com.xdev.osm_mobile.utils.horsligne.OfflineScan
import com.xdev.osm_mobile.utils.horsligne.SyncStatus

class SyncQueueActivity : AppCompatActivity() {

    private lateinit var offlineManager: OfflineManager
    private lateinit var adapter: SyncQueueAdapter
    private val scans = mutableListOf<OfflineScan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_queue)

        offlineManager = OfflineManager.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SyncQueueAdapter(scans) { scan ->
            // Réessayer : repasser en PENDING et rafraîchir
            offlineManager.updateScanStatus(scan.id, SyncStatus.PENDING)
            refreshList()
            // Optionnel : lancer une synchronisation immédiate
            offlineManager.syncPendingScans { _, _ -> refreshList() }
        }
        recyclerView.adapter = adapter



        findViewById<Button>(R.id.btnClearSynced).setOnClickListener {
            offlineManager.clearScansByStatus(SyncStatus.SYNCED)
            refreshList()
        }

        refreshList()
    }

    private fun refreshList() {
        scans.clear()
        scans.addAll(offlineManager.getAllScansWithStatus())
        adapter.notifyDataSetChanged()
    }
}