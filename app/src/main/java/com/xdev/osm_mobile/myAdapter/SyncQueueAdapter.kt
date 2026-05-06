package com.xdev.osm_mobile.myAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.models.OfflineOperation
import com.xdev.osm_mobile.models.OfflineScan
import com.xdev.osm_mobile.models.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class SyncItem {
    abstract val id: String
    abstract val status: SyncStatus
    abstract val timestamp: Long
    abstract val errorMessage: String?
    abstract val label: String

    data class Scan(val scan: OfflineScan) : SyncItem() {
        override val id = scan.id
        override val status = scan.status
        override val timestamp = scan.timestamp
        override val errorMessage = scan.errorMessage
        override val label = "Scan: ${scan.content}"
    }

    data class Operation(val op: OfflineOperation) : SyncItem() {
        override val id = op.id
        override val status = op.status
        override val timestamp = op.createdAt
        override val errorMessage = op.errorMessage
        override val label = "Action: ${op.method} ${op.url.substringAfterLast("/")}"
    }
}

class SyncQueueAdapter(
    private val items: List<SyncItem>,
    private val onRetry: (SyncItem) -> Unit
) : RecyclerView.Adapter<SyncQueueAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvError: TextView = itemView.findViewById(R.id.tvError)
        val btnRetry: Button = itemView.findViewById(R.id.btnRetry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_sync_scan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvContent.text = item.label
        val date = SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date(item.timestamp))
        holder.tvDate.text = date

        holder.tvStatus.text = when (item.status) {
            SyncStatus.PENDING -> " En attente"
            SyncStatus.SYNCED -> " Synchronisé"
            SyncStatus.ERROR -> " Erreur"
        }

        if (item.status == SyncStatus.ERROR) {
            holder.tvError.text = item.errorMessage ?: "Erreur inconnue"
            holder.tvError.visibility = View.VISIBLE
            holder.btnRetry.visibility = View.VISIBLE
            holder.btnRetry.setOnClickListener { onRetry(item) }
        } else {
            holder.tvError.visibility = View.GONE
            holder.btnRetry.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}
