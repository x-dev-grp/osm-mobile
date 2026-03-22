package com.xdev.osm_mobile.ui.sync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.utils.horsligne.OfflineScan
import com.xdev.osm_mobile.utils.horsligne.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncQueueAdapter(
    private val scans: List<OfflineScan>,
    private val onRetry: (OfflineScan) -> Unit
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
        val scan = scans[position]
        holder.tvContent.text = scan.content
        val date = SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date(scan.timestamp))
        holder.tvDate.text = date

        holder.tvStatus.text = when (scan.status) {
            SyncStatus.PENDING -> " En attente"
            SyncStatus.SYNCED -> " Synchronisé"
            SyncStatus.ERROR -> " Erreur"
        }

        if (scan.status == SyncStatus.ERROR) {
            holder.tvError.text = scan.errorMessage ?: "Erreur inconnue"
            holder.tvError.visibility = View.VISIBLE
            holder.btnRetry.visibility = View.VISIBLE
            holder.btnRetry.setOnClickListener { onRetry(scan) }
        } else {
            holder.tvError.visibility = View.GONE
            holder.btnRetry.visibility = View.GONE
        }
    }

    override fun getItemCount() = scans.size
}