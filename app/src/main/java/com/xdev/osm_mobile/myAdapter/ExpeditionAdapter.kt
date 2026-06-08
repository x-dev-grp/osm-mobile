package com.xdev.osm_mobile.myAdapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.databinding.ItemExpeditionBinding
import com.xdev.osm_mobile.models.ExpeditionDto

class ExpeditionAdapter(
    private val onClick: (ExpeditionDto) -> Unit
) : RecyclerView.Adapter<ExpeditionAdapter.ViewHolder>() {
    private var items = listOf<ExpeditionDto>()
    fun submitList(list: List<ExpeditionDto>) {
        items = list
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpeditionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    override fun getItemCount() = items.size
    class ViewHolder(
        private val binding: ItemExpeditionBinding,
        private val onClick: (ExpeditionDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(exp: ExpeditionDto) {
            binding.tvExpeditionNumber.text = exp.expeditionNumber ?: "-"
            binding.tvDestination.text = exp.destination ?: "-"
            binding.tvPlannedDate.text = exp.plannedShipDate ?: "-"
            binding.tvTotalQty.text = "Qté : ${exp.totalQuantity ?: 0}"
            binding.tvStatus.text = exp.status ?: "-"

            val color = when (exp.status?.uppercase()) {
                "DRAFT" -> android.graphics.Color.GRAY
                "READY" -> binding.root.context.getColor(R.color.colorAccent)
                "VALIDATED" -> binding.root.context.getColor(R.color.colorPrimary)
                "SHIPPED" -> binding.root.context.getColor(R.color.olive_green)
                "DELIVERED", "CLOSED" -> android.graphics.Color.DKGRAY
                "CANCELLED" -> android.graphics.Color.RED
                else -> android.graphics.Color.LTGRAY
            }
            binding.tvStatus.setBackgroundColor(color)

            binding.root.setOnClickListener { onClick(exp) }
        }
    }
}