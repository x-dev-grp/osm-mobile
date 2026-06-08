package com.xdev.osm_mobile.ui.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.databinding.ItemStockMovementBinding
import com.xdev.osm_mobile.database.entities.MovementEntity
import java.text.SimpleDateFormat
import java.util.*

class StockMovementAdapter : RecyclerView.Adapter<StockMovementAdapter.ViewHolder>() {
    private var items = listOf<MovementEntity>()
    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    fun submitList(list: List<MovementEntity>) {
        items = list
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockMovementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    override fun getItemCount() = items.size
    class ViewHolder(private val binding: ItemStockMovementBinding) : RecyclerView.ViewHolder(binding.root) {
        private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(m: MovementEntity) {
            val ctx = binding.root.context
            val typeLabel = when (m.typeMouvement.uppercase()) {
                "ENTREE"      -> "Entrée"
                "SORTIE"      -> "Conso"
                "AJUSTEMENT"  -> "Ajustement"
                "CONSOMMATION"-> "Conso"
                else          -> m.typeMouvement
            }
            val articleName = m.articleName ?: "Article ${m.articleId.take(8)}"
            binding.tvMovementTitle.text = "$typeLabel · $articleName"
            val qtySign = when (m.typeMouvement.uppercase()) {
                "ENTREE" -> "+${m.quantity}"
                "SORTIE", "CONSOMMATION" -> "-${m.quantity}"
                else -> "${m.quantity}"
            }
            val motif = m.motif?.takeIf { it.isNotBlank() } ?: "—"
            val time = m.dateMouvement?.let {
                runCatching { timeFmt.format(isoFmt.parse(it)!!) }.getOrNull()
            } ?: "--:--"
            binding.tvMovementSubtitle.text = "$motif · $qtySign u · $time"
            when (m.typeMouvement.uppercase()) {
                "ENTREE" -> {
                    binding.ivMovementIcon.setImageResource(R.drawable.ic_arrow_down_green)
                    binding.ivMovementIcon.imageTintList = null
                    binding.iconBox.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
                }
                "SORTIE", "CONSOMMATION" -> {
                    binding.ivMovementIcon.setImageResource(R.drawable.ic_arrow_up_red)
                    binding.ivMovementIcon.imageTintList = null
                    binding.iconBox.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                }
                "AJUSTEMENT" -> {
                    binding.ivMovementIcon.setImageResource(android.R.drawable.ic_menu_edit)
                    binding.ivMovementIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#E65100"))
                    binding.iconBox.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
                }
                else -> {
                    binding.ivMovementIcon.setImageResource(android.R.drawable.ic_menu_info_details)
                    binding.ivMovementIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#5E35B1"))
                    binding.iconBox.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EDE7F6"))
                }
            }
            val (statusText, statusColor, statusBgColor) = when (m.typeMouvement.uppercase()) {
                "ENTREE"                 -> Triple("Validé",     "#2E7D32", "#E8F5E9")
                "SORTIE", "CONSOMMATION" -> Triple("Validé",     "#2E7D32", "#E8F5E9")
                "AJUSTEMENT"             -> Triple("Motivé",     "#E65100", "#FFF3E0")
                else                     -> Triple("En cours",   "#5E35B1", "#EDE7F6")
            }

        }
    }
}