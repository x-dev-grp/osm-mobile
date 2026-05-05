package com.xdev.osm_mobile.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.scan.ScanItem
import com.xdev.osm_mobile.myAdapter.ScanHistoryAdapter

class ScanHistoryDialog : BottomSheetDialogFragment() {

    private var historyList: List<ScanItem> = emptyList()
    private var onItemClick: ((ScanItem) -> Unit)? = null

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            history: List<ScanItem>,
            onItemClick: (ScanItem) -> Unit
        ) {
            val dialog = ScanHistoryDialog()
            dialog.historyList = history
            dialog.onItemClick = onItemClick
            dialog.show(fragmentManager, "ScanHistoryDialog")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_scan_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)
        val btnClearHistory = view.findViewById<MaterialButton>(R.id.btnClearHistory)
        val layoutEmpty = view.findViewById<View>(R.id.layoutEmpty)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        if (historyList.isEmpty()) {
            // Afficher l'état vide
            layoutEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            btnClearHistory.visibility = View.GONE // Pas de bouton effacer si vide
        } else {
            // Afficher la liste et le bouton "Effacer"
            layoutEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            btnClearHistory.visibility = View.VISIBLE // Le bouton devient visible

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = ScanHistoryAdapter(historyList) { item ->
                onItemClick?.invoke(item)
                dismiss()
            }
        }

        // Bouton Fermer
        btnClose.setOnClickListener {
            dismiss()
        }

        // Bouton Effacer tout l'historique
        btnClearHistory.setOnClickListener {
            val manager = ScanHistoryManager(requireContext())
            manager.clearHistory() // Supprime tous les scans
            Toast.makeText(requireContext(), "Historique effacé", Toast.LENGTH_SHORT).show()
            dismiss() // Ferme le dialogue après effacement
        }
    }
}