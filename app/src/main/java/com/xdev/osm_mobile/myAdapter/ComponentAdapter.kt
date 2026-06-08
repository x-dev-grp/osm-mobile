package com.xdev.osm_mobile.myAdapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.models.LigneOFDto
import java.util.Locale

class ComponentAdapter(
    private val components: MutableList<LigneOFDto>,
    private val isEditable: Boolean = true,
    private val onAdjust: ((articleId: String, quantiteReelle: Double, motif: String) -> Unit)? = null
) : RecyclerView.Adapter<ComponentAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvComponentName)
        val tvInfo: TextView = itemView.findViewById(R.id.tvConsommationInfo)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val ivIcon: android.widget.ImageView = itemView.findViewById(R.id.ivComponentIcon)
        val iconBox: View = itemView.findViewById(R.id.iconBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_component, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comp = components[position]
        holder.tvName.text = comp.articleNom ?: comp.articleId ?: "-"
        
        val theo = comp.quantiteTheorique ?: 0.0
        val real = comp.quantiteReelle ?: 0.0
        holder.tvInfo.text = "Consommé: ${formatQuantity(real)} u · Théo: ${formatQuantity(theo)} u"
        val hasEcart = Math.abs(real - theo) > 0.01
        if (hasEcart) {
            holder.tvStatus.text = "Écart"
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#E65100"))
            holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF3E0"))
        } else {
            holder.tvStatus.text = "OK"
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
        }
        val name = comp.articleNom?.uppercase() ?: ""
        val iconRes = when {
            name.contains("BOUTEILLE") -> R.drawable.ic_production_order
            name.contains("BOUCHON") -> R.drawable.ic_quality_control
            name.contains("ETIQUETTE") -> R.drawable.ic_quality_control
            name.contains("CARTON") -> R.drawable.ic_stock_boxes
            else -> R.drawable.ic_stock_boxes
        }
        holder.ivIcon.setImageResource(iconRes)
        if (isEditable) {
            holder.itemView.setOnClickListener {
                showAdjustDialog(holder, comp, position)
            }
            holder.itemView.isClickable = true
            holder.itemView.isFocusable = true
            holder.itemView.setBackgroundResource(android.R.drawable.list_selector_background)
        } else {
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        }
    }
    private fun showAdjustDialog(holder: ViewHolder, comp: LigneOFDto, position: Int) {
        val context = holder.itemView.context
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_adjust_component, null)
        val etQuantite = dialogView.findViewById<EditText>(R.id.etQuantiteReelle)
        val etMotif = dialogView.findViewById<EditText>(R.id.etMotif)
        etQuantite.setText(comp.quantiteReelle?.toString() ?: comp.quantiteTheorique?.toString() ?: "")

        AlertDialog.Builder(context)
            .setTitle("Ajuster : ${comp.articleNom ?: comp.articleId}")
            .setView(dialogView)
            .setPositiveButton("Valider") { _, _ ->
                val qte = etQuantite.text.toString().toDoubleOrNull()
                val motif = etMotif.text.toString().trim()
                if (qte == null || qte < 0) {
                    // Validation simple
                    return@setPositiveButton
                }
                val articleId = comp.articleId ?: return@setPositiveButton
                onAdjust?.invoke(articleId, qte, motif)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    fun updateComponent(articleId: String, quantiteReelle: Double, motif: String) {
        val index = components.indexOfFirst { it.articleId == articleId }
        if (index >= 0) {
            val old = components[index]
            components[index] = old.copy(quantiteReelle = quantiteReelle, motifAjustement = motif)
            notifyItemChanged(index)
        }
    }

    override fun getItemCount() = components.size

    private fun formatQuantity(value: Double?): String =
        if (value != null) String.format(Locale.US, "%.2f", value) else "-"
}
