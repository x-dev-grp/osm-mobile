package com.xdev.osm_mobile.myAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.scan.ScanItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanHistoryAdapter(
    //houwa Kifech t  yorbrt w vusualisi data f interface  bin backend w frontend
    private val items: List<ScanItem>,
    private val onItemClick: (ScanItem) -> Unit
) : RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvContent: TextView = view.findViewById(R.id.tvContent) //affiche le contenue scannee
        val tvType: TextView =
            view.findViewById(R.id.tvType) //affiche le type de scan ; QRcode ou barcode
        val tvTime: TextView = view.findViewById(R.id.tvTime) //affiche la date , heur du scan
    }

    //cree la vue pour chaque element du liste
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_history, parent, false)
        return ViewHolder(view)
    }


    //pour remplit chaque ligne de la liste avec les donnees
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvContent.text = item.content
        holder.tvType.text = item.type

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvTime.text = dateFormat.format(Date(item.timestamp))

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size
}
