package com.xdev.osm_mobile.myAdapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.models.QCControlPointDTO

class ControlPointAdapter(
    private val points: List<QCControlPointDTO>,
    private val values: MutableMap<Int, String>,
    private val photos: MutableMap<Int, String?>,
    private val comments: MutableMap<Int, String>,
    private val onTakePhoto: (Int) -> Unit
) : RecyclerView.Adapter<ControlPointAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvControlPointName)
        val tvType: TextView = view.findViewById(R.id.tvControlPointType)
        val etValue: EditText = view.findViewById(R.id.etControlPointValue)
        val etComment: EditText = view.findViewById(R.id.etControlPointComment)
        val btnPhoto: Button = view.findViewById(R.id.btnControlPointPhoto)
        val ivPhoto: ImageView = view.findViewById(R.id.ivControlPointPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_control_point, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val point = points[position]

        holder.tvName.text = point.nom ?: "Point ${position + 1}"
        holder.tvType.text = when (point.type?.uppercase()) {
            "NUMERIC" -> {
                val min = point.minValue
                val max = point.maxValue
                if (min != null && max != null) "Numérique ($min - $max)"
                else if (min != null) "Numérique (min $min)"
                else if (max != null) "Numérique (max $max)"
                else "Numérique"
            }
            "BOOLEAN" -> "Booléen (ok/nok)"
            else -> "Texte libre"
        }

        // Valeur
        holder.etValue.setText(values[position] ?: "")
        holder.etValue.setOnFocusChangeListener { _, _ ->
            values[position] = holder.etValue.text.toString()
        }

        // Commentaire
        holder.etComment.setText(comments[position] ?: "")
        holder.etComment.setOnFocusChangeListener { _, _ ->
            comments[position] = holder.etComment.text.toString()
        }

        // Photo
        val currentPhoto = photos[position]
        if (currentPhoto != null) {
            try {
                val bytes = Base64.decode(currentPhoto, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.ivPhoto.setImageBitmap(bitmap)
                holder.ivPhoto.visibility = View.VISIBLE
                holder.btnPhoto.text = "Changer photo"
            } catch (e: Exception) {
                holder.ivPhoto.visibility = View.GONE
                holder.btnPhoto.text = "Prendre photo"
            }
        } else {
            holder.ivPhoto.visibility = View.GONE
            holder.btnPhoto.text = "Prendre photo"
        }

        holder.btnPhoto.setOnClickListener {
            onTakePhoto(position)
        }
    }

    override fun getItemCount() = points.size
}
