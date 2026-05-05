package com.xdev.osm_mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.xdev.osm_mobile.databinding.ActivityQrScannerBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.models.QrResolveResponse
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.scan.ScanHistoryManager
import com.xdev.osm_mobile.scan.ScanItem
import kotlinx.coroutines.launch
import retrofit2.HttpException

class QRScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrScannerBinding
    private var isFlashOn = false
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = true
    private var lastScanTime = 0L
    private val scanCooldown = 2000L
    private var mode: String? = null
    private val gson = Gson()

    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (!isScanning) return
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScanTime < scanCooldown) return
            lastScanTime = currentTime
            result.text?.let { barcodeText ->
                if (barcodeText.isNotBlank()) {
                    onScanSuccess(barcodeText, result.barcodeFormat)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupButtons()
        startScanAnimation()
        checkAndRequestCameraPermission()
        mode = intent.getStringExtra("mode")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Scanner QR Code"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupBarcodeView() {
        try {
            val formats = listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.PDF_417
            )
            binding.barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
            binding.barcodeView.setTorchOff()
            binding.barcodeView.decodeContinuous(barcodeCallback)
            handler.postDelayed({
                try { binding.barcodeView.resume() } catch (_: Exception) {}
                isScanning = true
            }, 500)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Erreur caméra: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupButtons() {
        binding.btnFlash.setOnClickListener { toggleFlash() }
        binding.btnGallery.setOnClickListener { openGallery() }
    }

    private fun startScanAnimation() {
        val animation = TranslateAnimation(
            TranslateAnimation.ABSOLUTE, 0f,
            TranslateAnimation.ABSOLUTE, 0f,
            TranslateAnimation.RELATIVE_TO_PARENT, 0f,
            TranslateAnimation.RELATIVE_TO_PARENT, 0.9f
        ).apply {
            duration = 2000
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        binding.scanLine.startAnimation(animation)
    }

    private fun toggleFlash() {
        try {
            isFlashOn = !isFlashOn
            if (isFlashOn) {
                binding.barcodeView.setTorchOn()
                binding.btnFlash.text = "Flash On"
            } else {
                binding.barcodeView.setTorchOff()
                binding.btnFlash.text = "Flash Off"
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Flash non disponible", Snackbar.LENGTH_SHORT).show()
            isFlashOn = false
        }
    }

    private fun openGallery() {
        Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try { binding.barcodeView.resume() } catch (_: Exception) { setupBarcodeView() }
            resumeScanning()
        }
    }

    override fun onPause() {
        super.onPause()
        try { binding.barcodeView.pause() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupBarcodeView()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupBarcodeView()
            } else {
                Toast.makeText(this, "Permission caméra requise", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    /**
     * Tente de parser le contenu scanné comme un objet QrResolveResponse.
     * Retourne l'objet si réussi (contient "entityType" et "entityId"), sinon null.
     */
    private fun tryParseDirectJson(content: String): QrResolveResponse? {
        return try {
            if (content.contains("\"entityType\"") && content.contains("\"entityId\"")) {
                gson.fromJson(content, QrResolveResponse::class.java)
            } else null
        } catch (e: Exception) {
            Log.w("QRScanner", "Le contenu n'est pas un JSON valide pour QrResolveResponse", e)
            null
        }
    }

    private fun extractPublicCode(scannedContent: String): String {
        val trimmed = scannedContent.trim()
        val qrHexPattern = Regex("\"qrHex\"\\s*:\\s*\"([A-F0-9]+)\"", RegexOption.IGNORE_CASE)
        val match = qrHexPattern.find(trimmed)
        if (match != null) {
            val code = match.groupValues[1].uppercase()
            Log.d("QRScanner", "Extraction depuis JSON: $code")
            return code
        }
        var result = trimmed
        if (result.startsWith("http")) result = result.substringAfterLast('/')
        if (result.contains(':')) result = result.substringAfterLast(':')
        result = result.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        Log.d("QRScanner", "Extraction classique: $result")
        return result
    }

    private suspend fun resolveWithFallback(scannedCode: String, isArticle: Boolean): QrResolveResponse? {
        // Tentative avec le code complet
        try {
            val response = if (isArticle) {
                RetrofitClient.instance.resolveArticle(scannedCode)
            } else {
                RetrofitClient.instance.resolveOF(scannedCode)
            }
            if (response.isSuccessful && response.body() != null) {
                return response.body()
            }
            // Si 404 et code plus long que 6, essayer les 6 premiers caractères
            if ((response.code() == 404 || !response.isSuccessful) && scannedCode.length > 6) {
                val shortCode = scannedCode.take(6)
                Log.d("QRScanner", "Code complet non trouvé, réessai avec: $shortCode")
                val retryResponse = if (isArticle) {
                    RetrofitClient.instance.resolveArticle(shortCode)
                } else {
                    RetrofitClient.instance.resolveOF(shortCode)
                }
                if (retryResponse.isSuccessful && retryResponse.body() != null) {
                    return retryResponse.body()
                }
            }
        } catch (e: HttpException) {
            if (e.code() == 404 && scannedCode.length > 6) {
                val shortCode = scannedCode.take(6)
                Log.d("QRScanner", "HttpException 404, réessai avec: $shortCode")
                try {
                    val retryResponse = if (isArticle) {
                        RetrofitClient.instance.resolveArticle(shortCode)
                    } else {
                        RetrofitClient.instance.resolveOF(shortCode)
                    }
                    if (retryResponse.isSuccessful && retryResponse.body() != null) {
                        return retryResponse.body()
                    }
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("QRScanner", "Erreur lors de l'appel API", e)
        }
        return null
    }

    private fun onScanSuccess(content: String, format: BarcodeFormat) {
        isScanning = false
        Log.i("QRScanner", "📷 CONTENU COMPLET DU QR CODE : $content")

        val historyManager = ScanHistoryManager(this)
        historyManager.addScanToHistory(ScanItem(content, "QR_CODE", format.toString(), System.currentTimeMillis()))

        // 1. Tentative de navigation directe via le JSON complet (cas OF et ARTICLE modernes)
        val directEntity = tryParseDirectJson(content)
        if (directEntity != null && directEntity.entityType in listOf("OF", "ARTICLE")) {
            Log.d("QRScanner", "Navigation directe via JSON intégré : ${directEntity.entityType} / ${directEntity.entityId}")
            navigateToEntity(directEntity)
            return
        }

        // 2. Sinon, extraire le code hexadécimal (anciens QR codes ou URLs)
        val scannedCode = extractPublicCode(content)
        Log.d("QRScanner", "Code extrait : $scannedCode")

        if (!scannedCode.matches(Regex("^[0-9A-F]{3,20}$"))) {
            Toast.makeText(this, "Code invalide : '$scannedCode'", Toast.LENGTH_LONG).show()
            resumeScanning()
            return
        }

        lifecycleScope.launch {
            val repository = OSMApplication.repository
            val isOnline = NetworkUtils.isInternetAvailable(this@QRScannerActivity)

            if (isOnline) {
                try {
                    // Pour les anciens QR codes, on détermine le type approximativement
                    val isArticle = content.contains("\"category\"") || content.contains("\"nom\"")
                    val resolveResponse = resolveWithFallback(scannedCode, isArticle)

                    if (resolveResponse != null) {
                        when (resolveResponse.entityType) {
                            "OF" -> repository.refreshOfDetail(resolveResponse.entityId)
                            "ARTICLE" -> repository.refreshArticle(resolveResponse.entityId)
                        }
                        navigateToEntity(resolveResponse)
                    } else {
                        Toast.makeText(this@QRScannerActivity, "Non trouvé sur le serveur: $scannedCode", Toast.LENGTH_SHORT).show()
                        resumeScanning()
                    }
                } catch (e: Exception) {
                    Log.e("QRScanner", "Erreur réseau", e)
                    Toast.makeText(this@QRScannerActivity, "Erreur de connexion", Toast.LENGTH_SHORT).show()
                    resumeScanning()
                }
            } else {
                // Mode hors ligne
                var cachedOf = repository.findOfByQrHex(scannedCode)
                if (cachedOf == null && scannedCode.length > 6) {
                    cachedOf = repository.findOfByQrHex(scannedCode.take(6))
                }
                if (cachedOf == null) {
                    cachedOf = repository.findOfByCode(scannedCode)
                }
                if (cachedOf != null) {
                    navigateToEntity(QrResolveResponse("OF", scannedCode, cachedOf.id, cachedOf.code ?: "", "OK", ""))
                    return@launch
                }

                var cachedArticle = repository.findArticleByQrHex(scannedCode)
                if (cachedArticle == null && scannedCode.length > 6) {
                    cachedArticle = repository.findArticleByQrHex(scannedCode.take(6))
                }
                if (cachedArticle == null) {
                    cachedArticle = repository.getArticleById(scannedCode)
                }
                if (cachedArticle != null) {
                    navigateToEntity(QrResolveResponse("ARTICLE", scannedCode, cachedArticle.id, cachedArticle.nom ?: "", "OK", "/article/detail"))
                    return@launch
                }

                Toast.makeText(this@QRScannerActivity, "Hors ligne : code inconnu", Toast.LENGTH_LONG).show()
                resumeScanning()
            }
        }
    }

    private fun navigateToEntity(response: QrResolveResponse) {
        when (response.entityType) {
            "OF" -> {
                if (mode == "history") {
                    val intent = QCHistoryDetailActivity.newIntent(this, response.entityId, response.label)
                    startActivity(intent)
                } else {
                    val intent = OfDetailActivity.newIntent(this, response.entityId)
                    startActivity(intent)
                }
                finish()
            }
            "ARTICLE" -> {
                val intent = ArticleDetailActivity.newIntent(this, response.entityId)
                startActivity(intent)
                finish()
            }
            else -> {
                Toast.makeText(this, "Type non supporté : ${response.entityType}", Toast.LENGTH_LONG).show()
                resumeScanning()
            }
        }
    }

    private fun resumeScanning() {
        isScanning = true
        binding.barcodeView.decodeContinuous(barcodeCallback)
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1001
        private const val REQUEST_CODE_GALLERY = 1002
    }
}