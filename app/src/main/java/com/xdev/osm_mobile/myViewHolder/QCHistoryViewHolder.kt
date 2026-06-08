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
        binding.tvControlPoint.text = result.controlPointNom ?: result.controlPointId ?: "Point inconnu"
        
        val isOk = result.statut == "OK" || result.statut?.equals("CONFORME", true) == true
        val statusText = if (isOk) "Conforme" else (result.statut ?: "NOK")
        binding.tvStatus.text = statusText
        
        if (isOk) {
            binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            binding.tvStatus.text = "✓ Conforme"
            binding.cardView.strokeColor = android.graphics.Color.parseColor("#E0E0E0")
            binding.pbValue.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7D32"))
        } else {
            binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FDECEA"))
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#C62828"))
            binding.tvStatus.text = "⚠️ NOK"
            binding.cardView.strokeColor = android.graphics.Color.parseColor("#FFCDD2")
            binding.pbValue.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C62828"))
        }

        if (result.type == "NUMERIC" && result.maxValue != null && result.minValue != null && result.valeur != null) {
            binding.llValueBar.visibility = android.view.View.VISIBLE
            binding.rlBooleanSection.visibility = android.view.View.GONE
            
            val min = result.minValue
            val max = result.maxValue
            
            binding.tvProgressMin.text = "$min"
            binding.tvProgressMax.text = "$max"
            
            if (!isOk) {
                binding.tvValueNumeric.text = "${result.valeur} · hors plage"
            } else {
                binding.tvValueNumeric.text = result.valeur
            }
            
            try {
                val valueStr = result.valeur.replace(",", ".")
                val valueDouble = Regex("[-+]?[0-9]*\\.?[0-9]+").find(valueStr)?.value?.toDouble() ?: 0.0
                
                val range = max - min
                val progress = if (range > 0) (((valueDouble - min) / range) * 100).toInt() else 0
                binding.pbValue.progress = progress.coerceIn(0, 100)
            } catch (e: Exception) {
                binding.pbValue.progress = 0
            }
        } else {
            binding.llValueBar.visibility = android.view.View.GONE
            binding.rlBooleanSection.visibility = android.view.View.VISIBLE
            
            binding.tvBooleanValue.text = result.valeur ?: "-"
            if (isOk) {
                binding.tvBooleanValue.setTextColor(android.graphics.Color.parseColor("#00838F"))
                binding.ivBooleanIcon.setImageResource(android.R.drawable.checkbox_on_background)
                binding.ivBooleanIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00838F"))
            } else {
                binding.tvBooleanValue.setTextColor(android.graphics.Color.parseColor("#C62828"))
                binding.ivBooleanIcon.setImageResource(android.R.drawable.ic_delete)
                binding.ivBooleanIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C62828"))
            }
        }

        if (!result.commentaire.isNullOrBlank()) {
            binding.llCommentContainer.visibility = android.view.View.VISIBLE
            binding.tvComment.text = result.commentaire
        } else {
            binding.llCommentContainer.visibility = android.view.View.GONE
        }
        
        if (!result.photo.isNullOrBlank()) {
            binding.tvPhoto.visibility = android.view.View.VISIBLE
            binding.tvPhoto.text = "1 photo"
            binding.tvPhoto.setOnClickListener {
                if (binding.ivPhotoImage.visibility == android.view.View.VISIBLE) {
                    binding.ivPhotoImage.visibility = android.view.View.GONE
                } else {
                    binding.ivPhotoImage.visibility = android.view.View.VISIBLE
                    try {
                        val base64Image = result.photo.substringAfter(",")
                        val decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
                        val decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        binding.ivPhotoImage.setImageBitmap(decodedByte)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            binding.tvPhoto.visibility = android.view.View.GONE
            binding.ivPhotoImage.visibility = android.view.View.GONE
        }
        
        binding.tvSignature.text = if (!result.signature.isNullOrBlank()) result.signature else "Opérateur"
    }
}
