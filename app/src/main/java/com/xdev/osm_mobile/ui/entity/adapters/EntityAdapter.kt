package com.xdev.osm_mobile.ui.entity.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.databinding.ItemEntityBinding
import com.xdev.osm_mobile.network.models.ColisDto
import com.xdev.osm_mobile.network.models.LotDto
import com.xdev.osm_mobile.network.models.OilTransactionDto
import com.xdev.osm_mobile.network.models.OrderFabricationDTO
import com.xdev.osm_mobile.network.models.PaletteDto

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
) : RecyclerView.Adapter<EntityAdapter.EntityViewHolder>() {

    inner class EntityViewHolder(val binding: ItemEntityBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        val binding = ItemEntityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EntityViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        val display = item.toDisplayModel()

        with(holder.binding) {
            tvTitle.text = display.title
            tvBadge.text = display.badge
            tvLine1.text = display.line1
            tvLine2.text = display.line2
            tvLine2.visibility = if (display.line2.isBlank()) View.GONE else View.VISIBLE

            val badgeColor = when (display.badge.uppercase()) {
                "COMPLETED", "SYNCED", "AVAILABLE", "DISPONIBLE", "LIBRE", "FREE" ->
                    ContextCompat.getColor(ctx, R.color.olive_green)

                "IN_PROGRESS", "EN COURS", "PLANNED", "PENDING", "USED", "UTILISEE" ->
                    ContextCompat.getColor(ctx, R.color.olive_gold)

                "CANCELLED", "ERROR", "BROKEN", "CASSEE", "FULL", "PLEIN" ->
                    ContextCompat.getColor(ctx, R.color.error)

                else -> ContextCompat.getColor(ctx, R.color.olive_dark)
            }

            tvBadge.backgroundTintList = ColorStateList.valueOf(badgeColor)
            root.setOnClickListener { onItemClick(item) }
        }
    }

    fun updateData(newItems: List<EntityItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun <T> updateTyped(list: List<T>, wrap: (T) -> EntityItem) {
        updateData(list.map(wrap))
    }
}

data class EntityDisplayModel(
    val title: String,
    val badge: String,
    val line1: String,
    val line2: String
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
        val skuCode = firstNonBlank(data.sku?.code, data.skuCode) ?: "-"
        val secondary = firstNonBlank(
            data.sku?.category,
            data.ligneConditionnement?.nom,
            data.ligneNom
        ) ?: "-"

        EntityDisplayModel(
            title = "OF-${data.code ?: "-"}",
            badge = data.statut ?: "-",
            line1 = "$skuCode - $secondary",
            line2 = String.format(
                "%.1f / %.1f u",
                data.quantiteBonne ?: 0.0,
                data.quantiteCible ?: 0.0
            )
        )
    }

    is EntityItem.OilTransaction -> EntityDisplayModel(
        title = data.transactionType ?: "Transaction",
        badge = data.transactionState ?: "-",
        line1 = data.createdAt?.take(10) ?: "Date inconnue",
        line2 = String.format(
            "%+.1f KG  |  %.3f TND",
            data.quantityKg ?: 0.0,
            data.totalPrice ?: 0.0
        )
    )
}

private fun firstNonBlank(vararg values: String?): String? =
    values.firstOrNull { !it.isNullOrBlank() }
