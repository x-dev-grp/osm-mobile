package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.xdev.osm_mobile.databinding.ActivityArticleDetailBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.models.*
import kotlinx.coroutines.launch
import android.widget.GridLayout
import android.graphics.Typeface
import androidx.core.content.ContextCompat

class ArticleDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArticleDetailBinding
    private lateinit var article: ArticleSecDto
    private var stock: StockSecDto? = null
    private var articleId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.abiooc_bg_light)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setupToolbar()
        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID) ?: ""
        if (articleId.isEmpty()) {
            Toast.makeText(this, "ID article manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadArticleDetails()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar?.title = "Détail article"
    }
    private fun loadArticleDetails() {
        Log.d("ArticleDetail", "loadArticleDetails for id=$articleId")
        val repository = OSMApplication.repository
        val gson = GsonBuilder().registerTypeAdapter(ArticleConfig::class.java, ArticleConfigDeserializer()).create()
        lifecycleScope.launch {
            if (NetworkUtils.isInternetAvailable(this@ArticleDetailActivity)) {
                try {
                    repository.refreshArticle(articleId)
                    repository.refreshStock(articleId)
                    Log.d("ArticleDetail", "Synchronisation OK depuis le backend")
                } catch (e: Exception) {
                    Log.e("ArticleDetail", "Erreur synchronisation", e)
                    Toast.makeText(
                        this@ArticleDetailActivity,
                        "Erreur de synchronisation, affichage du cache existant",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.d("ArticleDetail", "Mode hors ligne, affichage du cache local")
                Toast.makeText(
                    this@ArticleDetailActivity,
                    "Mode hors ligne – données en cache",
                    Toast.LENGTH_SHORT
                ).show()
            }
            val cachedArticle = repository.getArticleById(articleId)
            val cachedStock = repository.getStockByArticle(articleId)
            if (cachedArticle?.fullJson != null) {
                article = gson.fromJson(cachedArticle.fullJson, ArticleSecDto::class.java)
                stock = if (cachedStock != null) {
                    StockSecDto(
                        id = cachedStock.id,
                        quantiteActuelle = cachedStock.quantiteActuelle,
                        articleId = cachedStock.articleId,
                        emplacement = cachedStock.emplacementId?.let { empId ->
                            EmplacementStockDto(
                                id = empId,
                                code = cachedStock.emplacementCode,
                                nom = cachedStock.emplacementNom,
                                zone = cachedStock.emplacementZone,
                                typeEmplacement = cachedStock.emplacementType,
                                disponible = cachedStock.emplacementDisponible
                            )
                        }
                    )
                } else null
                displayArticleDetails()
            } else {
                Toast.makeText(
                    this@ArticleDetailActivity,
                    "Article non trouvé (aucune donnée locale)",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    private fun displayArticleDetails() {
        with(binding) {
            tvArticleName.text = article.nom ?: "-"
            tvSku.text = article.qrHex ?: "SKU-${article.id?.takeLast(5) ?: "00000"}"
            tvUnitTag.text = article.um ?: "Unité"
            tvStatusTag.text = if (article.actif == true) "Actif" else "Inactif"
            tvStatusTag.setBackgroundResource(
                if (article.actif == true) R.drawable.bg_tag_green else R.drawable.bg_status_nok
            )
            tvPieceTag.text = article.categorie ?: "Article"
            val quantity = stock?.quantiteActuelle ?: 0
            tvStockQuantity.text = String.format("%,d", quantity)
            val min = article.stockMinimum ?: 0
            val max = article.stockMaximum ?: 0
            tvStockMinMax.text = "$min / $max"
            val supplierName = article.fournisseur?.nom ?: "Fournisseur inconnu"
            tvSupplierName.text = supplierName
            tvSupplierInitials.text = if (supplierName.length >= 2) supplierName.take(2).uppercase() else "VE"
            tvSupplierDetails.text = "Fournisseur certifié — ${article.categorie ?: "Matériel"}"
            btnStockMovement.setOnClickListener {
                val intent = Intent(this@ArticleDetailActivity, StockMovementScannerActivity::class.java)
                intent.putExtra("articleId", article.id)
                startActivity(intent)
            }
            val emplacement = stock?.emplacement
            if (emplacement != null) {
                tvEmplacementCode.text = emplacement.code ?: "-"
                tvEmplacementNom.text = emplacement.nom ?: "-"
                tvEmplacementZone.text = emplacement.zone ?: "-"
                tvEmplacementType.text = emplacement.typeEmplacement ?: "-"
                tvEmplacementDisponible.text = if (emplacement.disponible == true) "Oui" else "Non"
            } else {
                tvEmplacementCode.text = "Aucun emplacement"
                tvEmplacementNom.text = "-"
                tvEmplacementZone.text = "-"
                tvEmplacementType.text = "-"
                tvEmplacementDisponible.text = "-"
            }
        }
        displayConfiguration()
    }

    private fun displayConfiguration() {
        binding.configGrid.removeAllViews()
        val config = article.configuration ?: return
        binding.tvConfigTitle.text = "CONFIGURATION — ${article.categorie?.uppercase() ?: "ARTICLE"}"
        when (config) {
            is UniteConfig -> {
                addGridItem("Matériau", config.material)
                addGridItem("Volume", config.volumeMl?.let { "$it ml" })
                addGridItem("Couleur", config.color)
                addGridItem("Col (neck)", config.neckType)
                addGridItem("Poids", config.weightGr?.let { "$it g" })
                addGridItem("Catégorie", article.categorie)
            }
            is ColisConfig -> {
                addGridItem("Unités/colis", config.unitsPerColis?.toString())
                config.dimensions?.let {
                    addGridItem("Dimensions", "${it.length}×${it.width}×${it.height}")
                }
                addGridItem("Poids max", config.maxWeightKg?.let { "$it kg" })
            }
            is PaletteConfig -> {
                addGridItem("Type", config.type)
                addGridItem("Matériau", config.material)
                addGridItem("Colis/couche", config.colisPerLayer?.toString())
                addGridItem("Couches", config.numberOfLayers?.toString())
                addGridItem("H. max", config.maxHeightCm?.let { "$it cm" })
            }
            is EmballageConfig -> {
                addGridItem("Sous-type", config.sousType)
                addGridItem("Matériau", config.material)
                config.dimensions?.let {
                    addGridItem("Dimensions", "${it.length}×${it.width}×${it.height}")
                }
            }
            is ConsommableConfig -> {
                addGridItem("Sous-type", config.sousType)
                addGridItem("Usage", config.usage)
                addGridItem("Unité", config.unit)
                addGridItem("Quantité", config.quantity?.toString())
            }
            is MatierePremiereConfig -> {
                addGridItem("Sous-type", config.sousType)
                addGridItem("Origine", config.origin)
                addGridItem("Qualité", config.qualityGrade)
                addGridItem("Densité", config.density?.toString())
            }
            is AccessoireConfig -> {
                addGridItem("Sous-type", config.sousType)
                addGridItem("Usage", config.usage)
                addGridItem("Garantie", config.garantieMois?.let { "$it mois" })
            }
        }
    }
    private fun addGridItem(label: String, value: String?) {
        if (value.isNullOrBlank()) return
        val context = this
        val itemLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p16 = 16.dpToPx()
            val p12 = 12.dpToPx()
            setPadding(p16, p12, p16, p12)
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            layoutParams = params
        }
        val labelTv = TextView(context).apply {
            text = label
            setTextColor(ContextCompat.getColor(context, R.color.abiooc_text_gray))
            textSize = 12f
        }
        val valueTv = TextView(context).apply {
            text = value
            setTextColor(ContextCompat.getColor(context, R.color.abiooc_text_dark))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 4.dpToPx(), 0, 0)
        }
        itemLayout.addView(labelTv)
        itemLayout.addView(valueTv)
        itemLayout.setBackgroundResource(R.drawable.bg_grid_item)
        binding.configGrid.addView(itemLayout)
    }
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    companion object {
        private const val EXTRA_ARTICLE_ID = "extra_article_id"
        fun newIntent(context: Context, articleId: String): Intent =
            Intent(context, ArticleDetailActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_ID, articleId)
            } }
}