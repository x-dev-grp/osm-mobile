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
import com.xdev.osm_mobile.models.AccessoireConfig
import com.xdev.osm_mobile.models.ArticleConfig
import com.xdev.osm_mobile.models.ArticleConfigDeserializer
import com.xdev.osm_mobile.models.ArticleSecDto
import com.xdev.osm_mobile.models.ColisConfig
import com.xdev.osm_mobile.models.ConsommableConfig
import com.xdev.osm_mobile.models.EmballageConfig
import com.xdev.osm_mobile.models.EmplacementStockDto
import com.xdev.osm_mobile.models.MatierePremiereConfig
import com.xdev.osm_mobile.models.PaletteConfig
import com.xdev.osm_mobile.models.StockSecDto
import com.xdev.osm_mobile.models.UniteConfig
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ArticleDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArticleDetailBinding
    private lateinit var article: ArticleSecDto
    private var stock: StockSecDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        val articleId = intent.getStringExtra(EXTRA_ARTICLE_ID)
        if (articleId.isNullOrEmpty()) {
            Toast.makeText(this, "ID article manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadArticleDetails(articleId)
    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar?.title = "Détail article"
    }
    private fun loadArticleDetails(articleId: String) {
        val repository = OSMApplication.repository
        val gson = GsonBuilder()
            .registerTypeAdapter(ArticleConfig::class.java, ArticleConfigDeserializer())
            .create()

        lifecycleScope.launch {
            // 1. Rafraîchir l'article et son stock si connecté
            if (NetworkUtils.isInternetAvailable(this@ArticleDetailActivity)) {
                try {
                    repository.refreshArticle(articleId)
                    repository.refreshStock(articleId)
                } catch (e: Exception) {
                    Log.e("ArticleDetail", "Erreur refresh", e)
                }
            }

            // 2. Charger depuis le cache local
            val cachedArticle = repository.getArticleById(articleId)
            val cachedStock = repository.getStockByArticle(articleId)

            if (cachedArticle?.fullJson != null) {
                article = gson.fromJson(cachedArticle.fullJson, ArticleSecDto::class.java)
                stock = if (cachedStock != null) {
                    // Convertir StockEntity → StockSecDto pour garder la compatibilité avec l'affichage existant
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
                Toast.makeText(this@ArticleDetailActivity, "Article non trouvé en cache", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun displayArticleDetails() {
        with(binding) {
            tvArticleName.text = article.nom ?: "-"
            tvCategory.text = article.categorie ?: "-"
            tvUnit.text = article.um ?: "-"
            tvStatus.text = if (article.actif == true) "Actif" else "Inactif"
            tvStockMin.text = article.stockMinimum?.toString() ?: "-"
            tvStockMax.text = article.stockMaximum?.toString() ?: "-"
            tvSupplier.text = article.fournisseur?.nom ?: "-"

            val quantity = stock?.quantiteActuelle ?: 0
            tvStockQuantity.text = "$quantity ${article.um ?: "u"}"

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
        binding.configurationContainer.removeAllViews()
        val config = article.configuration ?: return

        addSectionTitle("Configuration (${article.categorie})")

        when (config) {
            is UniteConfig -> {
                addDetailLine("Matériau", config.material)
                addDetailLine("Volume (ml)", config.volumeMl?.toString())
                addDetailLine("Couleur", config.color)
                addDetailLine("Type de col", config.neckType)
                addDetailLine("Poids (g)", config.weightGr?.toString())
            }
            is ColisConfig -> {
                addDetailLine("Unités par colis", config.unitsPerColis?.toString())
                config.dimensions?.let {
                    addDetailLine("Dimensions (L×l×h)", "${it.length}×${it.width}×${it.height}")
                }
                addDetailLine("Poids max (kg)", config.maxWeightKg?.toString())
            }
            is PaletteConfig -> {
                addDetailLine("Type", config.type)
                addDetailLine("Matériau", config.material)
                addDetailLine("Colis par couche", config.colisPerLayer?.toString())
                addDetailLine("Nombre de couches", config.numberOfLayers?.toString())
                addDetailLine("Hauteur max (cm)", config.maxHeightCm?.toString())
                addDetailLine("Palette client", if (config.clientSpecific == true) "Oui" else "Non")
            }
            is EmballageConfig -> {
                addDetailLine("Sous-type", config.sousType)
                addDetailLine("Matériau", config.material)
                config.dimensions?.let {
                    addDetailLine("Dimensions", "${it.length}×${it.width}×${it.height}")
                }
                addDetailLine("Branding client", if (config.clientBranding == true) "Oui" else "Non")
                addDetailLine("Poids (g)", config.poidsGrammes?.toString())
            }
            is ConsommableConfig -> {
                addDetailLine("Sous-type", config.sousType)
                addDetailLine("Usage", config.usage)
                addDetailLine("Unité", config.unit)
                addDetailLine("Quantité", config.quantity?.toString())
                addDetailLine("Temp. stockage (°C)", config.temperatureStockageCelsius?.toString())
            }
            is MatierePremiereConfig -> {
                addDetailLine("Sous-type", config.sousType)
                addDetailLine("Origine", config.origin)
                addDetailLine("Grade qualité", config.qualityGrade)
                addDetailLine("Densité", config.density?.toString())
                addDetailLine("Certifié bio", if (config.certifieBio == true) "Oui" else "Non")
            }
            is AccessoireConfig -> {
                addDetailLine("Sous-type", config.sousType)
                addDetailLine("Usage", config.usage)
                addDetailLine("Montage requis", if (config.necessiteMontage == true) "Oui" else "Non")
                addDetailLine("Garantie (mois)", config.garantieMois?.toString())
            }
        }
    }
    private fun addSectionTitle(title: String) {
        val textView = TextView(this).apply {
            text = title
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline6)
            setPadding(0, 24, 0, 8)
        }
        binding.configurationContainer.addView(textView)
    }

    private fun addDetailLine(label: String, value: String?) {
        if (value.isNullOrBlank()) return
        val labelTv = TextView(this).apply {
            text = "$label : "
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Subtitle2)
        }
        val valueTv = TextView(this).apply {
            text = value
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body1)
        }
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(labelTv)
            addView(valueTv)
        }
        binding.configurationContainer.addView(linearLayout)
    }

    companion object {
        private const val EXTRA_ARTICLE_ID = "extra_article_id"

        fun newIntent(context: Context, articleId: String): Intent =
            Intent(context, ArticleDetailActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_ID, articleId)
            }
    }
}
