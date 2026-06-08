package com.xdev.osm_mobile.myAdapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.databinding.ItemQcHistoryBinding
import com.xdev.osm_mobile.models.QCResultDTO
import com.xdev.osm_mobile.myViewHolder.QCHistoryViewHolder

class QCHistoryAdapter(private val history: List<QCResultDTO>) :
    RecyclerView.Adapter<QCHistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QCHistoryViewHolder {
        val binding = ItemQcHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QCHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QCHistoryViewHolder, position: Int) {
        holder.bind(history[position])
    }

    override fun getItemCount() = history.size
}
