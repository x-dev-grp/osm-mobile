package com.xdev.osm_mobile.myViewHolder

import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.databinding.ItemQcHistoryBinding
import com.xdev.osm_mobile.models.QCResultDTO

class QCHistoryViewHolder(private val binding: ItemQcHistoryBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(result: QCResultDTO) {
        val date = result.dateControle?.substring(0, 16) ?: "?"
        binding.tvDate.text = date
        binding.tvControlPoint.text = result.controlPointId?.take(12) ?: "Point inconnu"
        binding.tvValue.text = "Valeur : ${result.valeur ?: "-"}"
        binding.tvStatus.text = result.statut ?: "?"
        when (result.statut) {
            "OK" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_ok)
            "NOK" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_nok)
            else -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status)
        }
        binding.tvComment.text = if (!result.commentaire.isNullOrBlank()) "Commentaire : ${result.commentaire}" else ""
        binding.tvPhoto.text = if (!result.photo.isNullOrBlank()) "📷 Photo" else ""
        binding.tvSignature.text = if (!result.signature.isNullOrBlank()) "✍️ Signature" else ""
    }
}
