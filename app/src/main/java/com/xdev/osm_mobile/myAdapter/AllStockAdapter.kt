package com.xdev.osm_mobile.myAdapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xdev.osm_mobile.R
import com.xdev.osm_mobile.databinding.ItemStockDisplayBinding
import com.xdev.osm_mobile.database.dao.MainDao

class AllStockAdapter(
    private val onItemClick: (articleId: String) -> Unit
) : ListAdapter<MainDao.StockWithArticle, AllStockAdapter.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MainDao.StockWithArticle>() {
            override fun areItemsTheSame(
                oldItem: MainDao.StockWithArticle,
                newItem: MainDao.StockWithArticle
            ): Boolean = oldItem.articleId == newItem.articleId

            override fun areContentsTheSame(
                oldItem: MainDao.StockWithArticle,
                newItem: MainDao.StockWithArticle
            ): Boolean = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockDisplayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    class ViewHolder(private val binding: ItemStockDisplayBinding,private val onItemClick: (String) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val articleId = binding.root.tag as? String
                if (!articleId.isNullOrBlank()) {
                    onItemClick(articleId)
                }
            }
        }
        fun bind(stock: MainDao.StockWithArticle, isLast: Boolean) {
            val ctx = binding.root.context
            binding.tvArticleName.text = stock.articleName ?: "Article ${stock.articleId}"

            val qty = stock.quantiteActuelle ?: 0
            val um = stock.articleUm ?: "u"
            binding.tvStockQuantity.text = "Stock : ${formatQty(qty)} $um"

            val min = stock.articleStockMin ?: 0
            val (statusText, statusColor, bgColor) = when {
                qty <= min -> Triple("Critique", "#C62828", "#FDECEA")
                qty <= (min * 1.5).toInt() -> Triple("Bas", "#E65100", "#FFF3E0")
                else -> Triple("Normal", "#2E7D32", "#E8F5E9")
            }
            binding.tvStatus.text = statusText
            binding.tvStatus.setTextColor(Color.parseColor(statusColor))
            binding.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))

            val cat = stock.articleCategorie?.uppercase() ?: ""
            val iconRes = when {
                cat.contains("BOUTEILLE") || cat.contains("UNITE") -> R.drawable.ic_production_order
                cat.contains("BOUCHON") || cat.contains("ACCESSOIRE") -> R.drawable.ic_quality_control
                cat.contains("ETIQUETTE") || cat.contains("CONSOMMABLE") -> R.drawable.ic_quality_control
                cat.contains("CARTON") || cat.contains("EMBALLAGE") -> R.drawable.ic_stock_boxes
                cat.contains("PALETTE") -> R.drawable.ic_stock_boxes
                else -> R.drawable.ic_stock_boxes
            }
            binding.ivArticleIcon.setImageResource(iconRes)
            binding.iconBox.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
            binding.ivArticleIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(statusColor))
            binding.divider.visibility = if (isLast) View.GONE else View.VISIBLE
            val articleId = stock.articleId
            if (articleId.isNullOrBlank()) {
                binding.root.tag = null
                binding.root.alpha = 0.5f
            } else {
                binding.root.tag = articleId
                binding.root.alpha = 1f
            }
        }
        private fun formatQty(qty: Int): String {
            return if (qty >= 1000) {
                val k = qty / 1000
                val r = (qty % 1000).toString().padStart(3, '0')
                "$k $r"
            } else qty.toString()
        }
    }
}