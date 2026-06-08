package com.xdev.osm_mobile.myAdapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.databinding.ItemExpeditionLineBinding
import com.xdev.osm_mobile.models.ExpeditionLineDto

class ExpeditionLineAdapter(
    private val lines: List<ExpeditionLineDto>
) : RecyclerView.Adapter<ExpeditionLineAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpeditionLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lines[position])
    }
    override fun getItemCount() = lines.size

    class ViewHolder(private val binding: ItemExpeditionLineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(line: ExpeditionLineDto) {
            binding.tvArticleName.text = line.articleName ?: "-"
            binding.tvQuantity.text = "Qté : ${line.quantity ?: 0} ${line.unit ?: ""}"
            binding.tvLotNumber.text = "Lot : ${line.lotNumber ?: "Non défini"}"
            binding.tvOfCode.text = "OF : ${line.ofCode ?: "-"}"
        }
    }
}