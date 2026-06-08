package com.xdev.osm_mobile.myAdapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.databinding.ItemEntityBinding
import com.xdev.osm_mobile.databinding.ItemOfBinding
import com.xdev.osm_mobile.models.ColisDto
import com.xdev.osm_mobile.models.LotDto
import com.xdev.osm_mobile.models.OilTransactionDto
import com.xdev.osm_mobile.models.OrderFabricationDTO
import com.xdev.osm_mobile.models.PaletteDto

sealed class EntityItem {
    data class Lot(val data: LotDto) : EntityItem()
    data class Colis(val data: ColisDto) : EntityItem()
    data class Palette(val data: PaletteDto) : EntityItem()
    data class Of(val data: OrderFabricationDTO) : EntityItem()
    data class OilTransaction(val data: OilTransactionDto) : EntityItem()
}

class EntityAdapter(
    private var items: List<EntityItem> = emptyList(),
    private val onItemClick: (EntityItem) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_GENERIC = 0
        private const val TYPE_OF = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is EntityItem.Of) TYPE_OF else TYPE_GENERIC
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_OF) {
            OfViewHolder(ItemOfBinding.inflate(inflater, parent, false))
        } else {
            GenericViewHolder(ItemEntityBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        if (holder is OfViewHolder && item is EntityItem.Of) {
            bindOf(holder.binding, item.data)
            holder.itemView.setOnClickListener { onItemClick(item) }
        } else if (holder is GenericViewHolder) {
            bindGeneric(holder.binding, item)
            holder.itemView.setOnClickListener { onItemClick(item) }
        }
    }

    private fun bindOf(binding: ItemOfBinding, data: OrderFabricationDTO) {
        val ctx = binding.root.context
        with(binding) {
            tvOfNumber.text = "OF - ${data.code ?: "-"}"
            tvOfSku.text = data.skuCode ?: data.sku?.code ?: "-"
            val start = data.dateDebutPrevue ?: "-"
            val end = data.dateFinPrevue ?: "-"
            tvOfDates.text = "$start → $end"
            val cible = data.quantiteCible ?: 0.0
            val bonne = data.quantiteBonne ?: 0.0
            val nc = data.quantiteNC ?: 0.0
            tvOfTarget.text = String.format("%.0f u.", cible)
            if (nc > 0) {
                tvLabelNC.visibility = View.VISIBLE
                tvOfNC.visibility = View.VISIBLE
                tvOfNC.text = String.format("%.0f u.", nc)
            } else {
                tvLabelNC.visibility = View.GONE
                tvOfNC.visibility = View.GONE
            }
            val percent = if (cible > 0) ((bonne / cible) * 100).toInt() else 0
            pbOfProgress.progress = percent
            tvOfBonnes.text = String.format("%.0f bonnes", bonne)
            tvOfPercent.text = "$percent%"
            val status = data.statut?.uppercase() ?: "PLANNED"
            tvOfStatus.text = status.replace("_", " ")
            
            val (bg, txt) = when (status) {
                "EN_COURS", "IN_PROGRESS", "RUNNING" -> R.color.status_in_progress_bg to R.color.status_in_progress_text
                "PLANNED", "PLANIFIE" -> R.color.status_planned_bg to R.color.status_planned_text
                "SUSPENDED", "PAUSE", "SUSPENDU" -> R.color.status_suspended_bg to R.color.status_suspended_text
                "COMPLETED", "TERMINE", "FINISHED" -> R.color.status_completed_bg to R.color.status_completed_text
                "CANCELLED", "ANNULE" -> R.color.status_cancelled_bg to R.color.status_cancelled_text
                else -> R.color.abiooc_stroke to R.color.abiooc_text_gray
            }
            tvOfStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, bg))
            tvOfStatus.setTextColor(ContextCompat.getColor(ctx, txt))
            pbOfProgress.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, txt))
            tvOfLine.text = data.lotVracNom  ?: "Ligne -"
            tvOfLot.text = data.lotVracId ?: "-"
        }
    }

    private fun bindGeneric(binding: ItemEntityBinding, item: EntityItem) {
        val ctx = binding.root.context
        val display = item.toDisplayModel()
        with(binding) {
            tvTitle.text = display.title
            tvBadge.text = display.badge
            tvLine1.text = display.line1

            val badgeColor = when (display.badge.uppercase()) {
                "COMPLETED", "SYNCED", "AVAILABLE", "DISPONIBLE", "LIBRE", "FREE", "EN COURS" ->
                    ContextCompat.getColor(ctx, R.color.olive_green)
                "IN_PROGRESS", "PLANNED", "PENDING", "USED", "UTILISEE", "PAUSE" ->
                    ContextCompat.getColor(ctx, R.color.olive_gold)
                "CANCELLED", "ERROR", "BROKEN", "CASSEE", "FULL", "PLEIN" ->
                    ContextCompat.getColor(ctx, R.color.error)
                else -> ContextCompat.getColor(ctx, R.color.olive_dark)
            }

            tvBadge.backgroundTintList = ColorStateList.valueOf(badgeColor)

            if (display.progressPercent != null) {
                tvLine2.visibility = View.GONE
                layoutProgress.visibility = View.VISIBLE
                pbProgress.progress = display.progressPercent
                pbProgress.progressTintList = ColorStateList.valueOf(badgeColor)
                tvProgressText.text = display.progressText
                tvProgressPercent.text = "${display.progressPercent}%"
            } else {
                layoutProgress.visibility = View.GONE
                tvLine2.text = display.line2
                tvLine2.visibility = if (display.line2.isBlank()) View.GONE else View.VISIBLE
            }
        }
    }

    fun updateData(newItems: List<EntityItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class GenericViewHolder(val binding: ItemEntityBinding) : RecyclerView.ViewHolder(binding.root)
    inner class OfViewHolder(val binding: ItemOfBinding) : RecyclerView.ViewHolder(binding.root)
}

data class EntityDisplayModel(
    val title: String,
    val badge: String,
    val line1: String,
    val line2: String,
    val progressPercent: Int? = null,
    val progressText: String? = null
)

private fun EntityItem.toDisplayModel(): EntityDisplayModel = when (this) {
    is EntityItem.Lot -> EntityDisplayModel(
        title = "LOT #${data.lotNumber ?: "-"}",
        badge = data.status ?: "-",
        line1 = data.supplier?.name ?: "Fournisseur inconnu",
        line2 = String.format("%.0f KG", (data.oliveQuantity ?: 0.0) + (data.oilQuantity ?: 0.0))
    )
    is EntityItem.Colis -> EntityDisplayModel(
        title = data.name ?: "Colis sans nom",
        badge = "${data.stockQuantity ?: 0} unites",
        line1 = data.description ?: "Aucune description",
        line2 = String.format("%.3f TND", data.sellingPrice ?: 0.0)
    )
    is EntityItem.Palette -> EntityDisplayModel(
        title = data.code ?: "PAL--",
        badge = data.status ?: "-",
        line1 = data.description ?: "Aucune description",
        line2 = ""
    )
    is EntityItem.Of -> {
        val skuCode = data.skuCode ?: data.sku?.code ?: "-"
        val qteBonne = data.quantiteBonne ?: 0.0
        val qteCible = data.quantiteCible ?: 0.0
        val percent = if (qteCible > 0) ((qteBonne / qteCible) * 100).toInt() else 0
        EntityDisplayModel(
            title = "OF-${data.code ?: "-"}",
            badge = data.statut ?: "-",
            line1 = skuCode,
            line2 = "",
            progressPercent = percent,
            progressText = String.format("%.0f / %.0f unités", qteBonne, qteCible)
        )
    }
    is EntityItem.OilTransaction -> EntityDisplayModel(
        title = data.transactionType ?: "Transaction",
        badge = data.transactionState ?: "-",
        line1 = data.createdAt?.take(10) ?: "Date inconnue",
        line2 = String.format("%+.1f KG  |  %.3f TND", data.quantityKg ?: 0.0, data.totalPrice ?: 0.0)
    )
}
