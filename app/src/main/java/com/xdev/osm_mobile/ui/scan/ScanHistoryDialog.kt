package com.xdev.osm_mobile.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.xdev.osm_mobile.R

class ScanHistoryDialog : BottomSheetDialogFragment() {

    private var historyList: List<ScanItem> = emptyList()
    private var onItemClick: ((ScanItem) -> Unit)? = null

    companion object {
        fun show( // pour afficher le dialogue
            fragmentManager: androidx.fragment.app.FragmentManager,
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
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        if (historyList.isEmpty()) {
            dismiss()
            return //  //si historique vide , le dialogue se ferme immedia

        }
        // Configuration normale si la liste n'est pas vide
        recyclerView.layoutManager = LinearLayoutManager(context)
        tvEmpty.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        recyclerView.adapter = ScanHistoryAdapter(historyList) { item ->
            onItemClick?.invoke(item)
            dismiss()
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }
}


