package com.xdev.osm_mobile.myAdapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
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
        val tvTheoretical: TextView = itemView.findViewById(R.id.tvTheoreticalQty)
        val tvReal: TextView = itemView.findViewById(R.id.tvRealQty)
        val tvMotif: TextView = itemView.findViewById(R.id.tvMotif)
        val btnAdjust: ImageButton = itemView.findViewById(R.id.btnAdjust)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_component, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comp = components[position]
        holder.tvName.text = comp.articleNom ?: comp.articleId ?: "-"
        holder.tvTheoretical.text = "Th: ${formatQuantity(comp.quantiteTheorique)}"
        holder.tvReal.text = "Réel: ${formatQuantity(comp.quantiteReelle)}"

        // Motif
        if (!comp.motifAjustement.isNullOrBlank()) {
            holder.tvMotif.text = "Motif : ${comp.motifAjustement}"
            holder.tvMotif.visibility = View.VISIBLE
        } else {
            holder.tvMotif.visibility = View.GONE
        }

        // Bouton ajuster
        holder.btnAdjust.visibility = if (isEditable) View.VISIBLE else View.GONE
        holder.btnAdjust.setOnClickListener {
            showAdjustDialog(holder, comp, position)
        }
    }

    private fun showAdjustDialog(holder: ViewHolder, comp: LigneOFDto, position: Int) {
        val context = holder.itemView.context
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_adjust_component, null)
        val etQuantite = dialogView.findViewById<EditText>(R.id.etQuantiteReelle)
        val etMotif = dialogView.findViewById<EditText>(R.id.etMotif)

        // Pré-remplir avec la valeur actuelle
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

    // Met à jour localement un composant après succès API
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
