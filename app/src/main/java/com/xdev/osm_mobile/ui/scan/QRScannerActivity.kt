package com.xdev.osm_mobile.ui.scan

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.xdev.osm_mobile.databinding.ActivityQrScannerBinding
import com.xdev.osm_mobile.network.models.ScanData
import com.xdev.osm_mobile.utils.horsligne.NetworkUtils
import com.xdev.osm_mobile.utils.horsligne.OfflineManager

/**
 * Activité de scan QR Code avec support des codes-barres
 * et saisie manuelle en cas d'échec.
 */
class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private var isFlashOn = false
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = true
    private var lastScanTime = 0L
    private val scanCooldown = 2000L // 2 secondes entre chaque scan

    // Callback pour la lecture automatique des codes-barres
    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (!isScanning) return

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScanTime < scanCooldown) {
                return
            }

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

        // Vérifier et demander la permission caméra
        checkAndRequestCameraPermission()
    }

    // Barre d'action principale
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Scanner QR Code"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // Configuration du lecteur de codes-barres
    private fun setupBarcodeView() {
        try {
            // Formats supportés
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

            // Flash désactivé par défaut
            binding.barcodeView.setTorchOff()

            // Démarrage du scan continu
            binding.barcodeView.decodeContinuous(barcodeCallback)

            // Redémarrage forcé de la caméra
            handler.postDelayed({
                try {
                    binding.barcodeView.resume()
                    isScanning = true
                } catch (e: Exception) {
                    // Ignorer
                }
            }, 500)

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Erreur caméra: ${e.message}", Snackbar.LENGTH_LONG)
                .setAction("Saisie manuelle") { showManualEntryDialog() }
                .show()
        }
    }

    // Configuration des boutons
    private fun setupButtons() {
        // Bouton flash
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        // Bouton galerie
        binding.btnGallery.setOnClickListener {
            openGallery()
        }

        // Nouveau bouton pour la saisie manuelle
        binding.btnManualEntry.setOnClickListener {
            showManualEntryDialog()
        }
    }

    // Animation de la ligne de scan
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

    // Activation/désactivation du flash
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

    // Ouvre la galerie (fonctionnalité à venir)
    private fun openGallery() {
        Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show()
    }

    // Traite un scan réussi
    private fun onScanSuccess(content: String, format: BarcodeFormat) {
        isScanning = false

        // Créer l'objet ScanData
        val scanData = ScanData(
            content = content,
            type = if (format == BarcodeFormat.QR_CODE) "QR_CODE" else "BARCODE",
            format = format.toString(),
            timestamp = System.currentTimeMillis()
        )

        // Sauvegarder dans l'historique local
        val historyManager = ScanHistoryManager(this)
        historyManager.addScanToHistory(
            ScanItem(
                content = content,
                type = scanData.type,
                format = scanData.format,
                timestamp = scanData.timestamp
            )
        )

        // Gestion hors ligne
        if (!NetworkUtils.isInternetAvailable(this)) {
            OfflineManager.getInstance(this).saveScanOffline(scanData)
        }

        // Retourner le résultat
        returnScanResult(content, scanData.type, scanData.format)
    }

    // Retourne le résultat à l'activité appelante
    private fun returnScanResult(content: String, scanType: String, format: String) {
        val resultIntent = Intent().apply {
            putExtra("scanned_content", content)
            putExtra("scan_type", scanType)
            putExtra("scan_format", format)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    // Affiche la boîte de dialogue pour la saisie manuelle
    private fun showManualEntryDialog() {
        val input = EditText(this)
        input.hint = "Entrez le code manuellement"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Saisie manuelle")
            .setMessage("Si le scan échoue, entrez le code ci-dessous :")
            .setView(layout)
            .setPositiveButton("Valider") { _, _ ->
                val code = input.text.toString().trim()
                if (code.isNotEmpty()) {
                    processManualCode(code)
                } else {
                    Toast.makeText(this, "Code vide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // Traite le code saisi manuellement comme un scan
    private fun processManualCode(code: String) {
        // Créer un ScanData avec le type "MANUAL"
        val scanData = ScanData(
            content = code,
            type = "MANUAL",
            format = "MANUAL",
            timestamp = System.currentTimeMillis()
        )

        // Sauvegarder dans l'historique local
        val historyManager = ScanHistoryManager(this)
        historyManager.addScanToHistory(
            ScanItem(
                content = code,
                type = scanData.type,
                format = scanData.format,
                timestamp = scanData.timestamp
            )
        )

        // Gestion hors ligne
        if (!NetworkUtils.isInternetAvailable(this)) {
            OfflineManager.getInstance(this).saveScanOffline(scanData)
        }

        // Retourner le résultat
        returnScanResult(code, scanData.type, scanData.format)
    }

    // Reprend le scan après un résultat
    private fun resumeScanning() {
        isScanning = true
        binding.barcodeView.setStatusText("")
        binding.barcodeView.decodeContinuous(barcodeCallback)
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                binding.barcodeView.resume()
                resumeScanning()
            } catch (e: Exception) {
                // Si barcodeView n'est pas encore initialisé, on le fait maintenant
                setupBarcodeView()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.barcodeView.pause()
        } catch (e: Exception) {
            // Ignorer
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // Gestion de la permission caméra
    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setupBarcodeView()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1001
        private const val REQUEST_CODE_GALLERY = 1002
    }
}