package com.xdev.osm_mobile.myAdapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.databinding.ItemExpeditionLineFifoBinding
import com.xdev.osm_mobile.models.ExpeditionLineDto

class ExpeditionLineFifoAdapter(private val lines: List<ExpeditionLineDto>) :
    RecyclerView.Adapter<ExpeditionLineFifoAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemExpeditionLineFifoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpeditionLineFifoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = lines[position]
        holder.binding.tvArticleName.text = line.articleName ?: "-"
        holder.binding.tvQuantity.text = "Qté : ${line.quantity ?: 0} ${line.unit ?: ""}"
        holder.binding.tvLotNumber.text = "Lot : ${line.lotNumber ?: "Non défini"}"
    }

    override fun getItemCount() = lines.size
}