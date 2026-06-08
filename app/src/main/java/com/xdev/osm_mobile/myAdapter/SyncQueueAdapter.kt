package com.xdev.osm_mobile.myAdapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.databinding.ItemSyncScanBinding
import com.xdev.osm_mobile.models.OfflineOperation
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
    data class Operation(val op: OfflineOperation) : SyncItem() {
        override val id = op.id
        override val status = op.status
        override val timestamp = op.createdAt
        override val errorMessage = op.errorMessage
        override val label = "Action: ${op.method} ${op.url.substringAfterLast("/")}"
    }
}
class SyncQueueAdapter(
    private var items: List<SyncItem>,
    private val onRetry: (SyncItem) -> Unit,
    private val onDelete: (SyncItem) -> Unit
) : RecyclerView.Adapter<SyncQueueAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSyncScanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSyncScanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        
        with(holder.binding) {
            tvContent.text = item.label
            val date = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault()).format(Date(item.timestamp))
            tvDate.text = date
            when (item.status) {
                SyncStatus.PENDING -> {
                    tvStatus.text = "En attente"
                    tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sync_pending_bg))
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.sync_pending_text))
                    iconContainer.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sync_pending_bg))
                    ivStatusIcon.setImageResource(R.drawable.ic_history)
                    ivStatusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sync_pending_text))
                    tvError.visibility = View.GONE
                    btnRetry.visibility = View.GONE
                }
                SyncStatus.SYNCED -> {
                    tvStatus.text = "Synchronisé"
                    tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sync_success_bg))
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.sync_success_text))
                    iconContainer.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sync_success_bg))
                    ivStatusIcon.setImageResource(R.drawable.ic_verification)
                    ivStatusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sync_success_text))
                    tvError.visibility = View.GONE
                    btnRetry.visibility = View.GONE
                }
                SyncStatus.ERROR -> {
                    tvStatus.text = "Erreur"
                    tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sync_error_bg))
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.sync_error_text))
                    iconContainer.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sync_error_bg))
                    ivStatusIcon.setImageResource(R.drawable.ic_info)
                    ivStatusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sync_error_text))
                    tvError.text = "× ${item.errorMessage ?: "Erreur serveur"}"
                    tvError.visibility = View.VISIBLE
                    btnRetry.visibility = View.VISIBLE
                    btnRetry.setOnClickListener { onRetry(item) }
                }
            }

            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<SyncItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
