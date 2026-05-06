package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.xdev.osm_mobile.databinding.ActivityQrScannerBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.ArticleSecDto
import com.xdev.osm_mobile.models.OfflineOperation
import com.xdev.osm_mobile.models.StockSecDto
import com.xdev.osm_mobile.models.SyncRequest
import com.xdev.osm_mobile.network.ApiService
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.regex.Pattern
class StockMovementScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrScannerBinding
    private var isScanning = true
    private var scannedArticle: ArticleSecDto? = null
    private var scannedStock: StockSecDto? = null
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBarcodeView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mouvement de stock"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    private fun setupBarcodeView() {
        val formats = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128, BarcodeFormat.EAN_13)
        binding.barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        binding.barcodeView.decodeContinuous(callback)
        binding.barcodeView.setTorchOff()
    }
    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (!isScanning) return
            isScanning = false
            result.text?.let { onScanSuccess(it) }
        }
    }
    private fun onScanSuccess(scannedContent: String) {
        val publicCode = extractPublicCode(scannedContent)
        resolveArticle(publicCode)
    }
    private fun extractPublicCode(content: String): String {
        val jsonPattern = Pattern.compile("\"qrHex\"\\s*:\\s*\"([A-F0-9]+)\"")
        val matcher = jsonPattern.matcher(content)
        if (matcher.find()) {
            return matcher.group(1)
        }
        if (content.startsWith("http")) {
            return content.substringAfterLast('/')
        }
        return content.trim()
    }
    private fun resolveArticle(code: String) {
        lifecycleScope.launch {
            try {
                val resolveResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.resolveArticle(code)
                }
                if (!resolveResponse.isSuccessful || resolveResponse.body() == null) {
                    Toast.makeText(this@StockMovementScannerActivity, "Article non trouvé", Toast.LENGTH_LONG).show()
                    resumeScanning()
                    return@launch
                }
                val articleId = resolveResponse.body()!!.entityId

                val articleDeferred = async { RetrofitClient.instance.getArticleById(articleId) }
                val stockDeferred = async { RetrofitClient.instance.getStockByArticle(articleId) }

                val articleResp = articleDeferred.await()
                val stockResp = stockDeferred.await()

                if (articleResp.isSuccessful && articleResp.body() != null) {
                    scannedArticle = articleResp.body()!!
                    scannedStock = if (stockResp.isSuccessful) stockResp.body() else null
                    showMovementDialog()
                } else {
                    Toast.makeText(this@StockMovementScannerActivity, "Erreur chargement article", Toast.LENGTH_LONG).show()
                    resumeScanning()
                }
            } catch (e: Exception) {
                Toast.makeText(this@StockMovementScannerActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                resumeScanning()
            }
        }
    }
    private fun showMovementDialog() {
        val options = arrayOf(" Entrée", "Sortie", "Ajustement")
        AlertDialog.Builder(this)
            .setTitle("Mouvement pour ${scannedArticle?.nom}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showQuantityDialog("Entrée", TypeMouvement.ENTREE)
                    1 -> showQuantityDialog("Sortie", TypeMouvement.SORTIE)
                    2 -> showAdjustmentDialog()
                    else -> {}
                }
            }
            .setOnDismissListener { resumeScanning() }
            .show()
    }
    private fun createQuantityView(hintText: String): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }
        val etQuantite = EditText(this).apply {
            hint = hintText
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val etMotif = EditText(this).apply {
            hint = "Motif (optionnel)"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 24 }
        }

        layout.addView(etQuantite)
        layout.addView(etMotif)

        return layout
    }
    private fun showQuantityDialog(title: String, type: TypeMouvement) {
        val view = createQuantityView("Quantité")
        val etQuantite = view.getChildAt(0) as EditText
        val etMotif = view.getChildAt(1) as EditText

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton("Valider") { _, _ ->
                val quantite = etQuantite.text.toString().toIntOrNull()
                val motif = etMotif.text.toString()
                if (quantite == null || quantite <= 0) {
                    Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                performMovement(type, quantite, motif)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    private fun performMovement(type: TypeMouvement, quantite: Int, motif: String) {
        val articleId = scannedArticle!!.id!!
        val request = ApiService.StockMovementRequest(quantite, motif)
        val bodyJson = gson.toJson(request)

        if (NetworkUtils.isInternetAvailable(this)) {
            lifecycleScope.launch {
                try {
                    val response = when (type) {
                        TypeMouvement.ENTREE -> RetrofitClient.instance.entreeStock(articleId, request)
                        TypeMouvement.SORTIE -> RetrofitClient.instance.sortieStock(articleId, request)
                    }
                    if (response.isSuccessful) {
                        Toast.makeText(this@StockMovementScannerActivity, "Mouvement effectué", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Erreur ${response.code()}"
                        Toast.makeText(this@StockMovementScannerActivity, errorMsg, Toast.LENGTH_LONG).show()
                        resumeScanning()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@StockMovementScannerActivity, "Erreur réseau: ${e.message}", Toast.LENGTH_LONG).show()
                    resumeScanning()
                }
            }
        } else {
            val operationId = UUID.randomUUID().toString()
            val url = when (type) {
                TypeMouvement.ENTREE -> "/api/inventaire/stocks/$articleId/entree"
                TypeMouvement.SORTIE -> "/api/inventaire/stocks/$articleId/sortie"
            }
            val syncRequest = SyncRequest(
                operationId = operationId,
                url = url,
                method = "PUT",
                body = bodyJson
            )
            val operation = OfflineOperation(
                operationId = operationId,
                url = url,
                method = "PUT",
                body = bodyJson
            )
            OfflineManager.getInstance(this).saveOperation(operation)
            Toast.makeText(this, "Mouvement enregistré localement, sera synchronisé plus tard", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showAdjustmentDialog() {
        val view = createQuantityView("Nouvelle quantité")
        val etQuantite = view.getChildAt(0) as EditText
        val etMotif = view.getChildAt(1) as EditText

        AlertDialog.Builder(this)
            .setTitle("Ajustement de stock")
            .setView(view)
            .setPositiveButton("Valider") { _, _ ->
                val nouvelleQuantite = etQuantite.text.toString().toIntOrNull()
                val motif = etMotif.text.toString()
                if (nouvelleQuantite == null || nouvelleQuantite < 0) {
                    Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                performAdjustment(nouvelleQuantite, motif)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun performAdjustment(nouvelleQuantite: Int, motif: String) {
        val articleId = scannedArticle!!.id!!
        val request = ApiService.StockAdjustmentRequest(nouvelleQuantite, motif)
        val bodyJson = gson.toJson(request)

        if (NetworkUtils.isInternetAvailable(this)) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.ajusterStock(articleId, request)
                    if (response.isSuccessful) {
                        Toast.makeText(this@StockMovementScannerActivity, "Ajustement effectué", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@StockMovementScannerActivity, "Erreur: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                        resumeScanning()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@StockMovementScannerActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                    resumeScanning()
                }
            }
        } else {
            val operationId = UUID.randomUUID().toString()
            val url = "/api/inventaire/stocks/$articleId/ajuster"
            val syncRequest = SyncRequest(
                operationId = operationId,
                url = url,
                method = "PUT",
                body = bodyJson
            )
            val operation = OfflineOperation(
                operationId = operationId,
                url = url,
                method = "PUT",
                body = bodyJson
            )
            OfflineManager.getInstance(this).saveOperation(operation)
            Toast.makeText(this, "Ajustement enregistré localement, sera synchronisé plus tard", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun resumeScanning() {
        isScanning = true
        binding.barcodeView.decodeContinuous(callback)
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.barcodeView.pause()
    }

    enum class TypeMouvement { ENTREE, SORTIE }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, StockMovementScannerActivity::class.java)
    }
}
