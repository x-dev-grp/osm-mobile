package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xdev.osm_mobile.databinding.ActivityMainBinding
import com.xdev.osm_mobile.network.models.ScanData
import com.xdev.osm_mobile.ui.entity.EntityListActivity
import com.xdev.osm_mobile.ui.entity.EntityListType
import com.xdev.osm_mobile.ui.login.LoginActivity
import com.xdev.osm_mobile.ui.scan.QRScannerActivity
import com.xdev.osm_mobile.ui.scan.ScanHistoryDialog
import com.xdev.osm_mobile.ui.scan.ScanHistoryManager
import com.xdev.osm_mobile.ui.scan.ScanItem
import com.xdev.osm_mobile.ui.sync.SyncQueueActivity
import com.xdev.osm_mobile.utils.horsligne.NetworkUtils
import com.xdev.osm_mobile.utils.horsligne.OfflineManager

/**
 * MainActivity - Écran principal avec fonctionnalités de scan QR code
 * et gestion hors ligne des scans
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var scanHistoryManager: ScanHistoryManager
    private lateinit var offlineManager: OfflineManager
    private var isFabMenuExpanded = false
    private var connectivityListener: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        scanHistoryManager = ScanHistoryManager(this)
        offlineManager = OfflineManager.getInstance(this)

        setupUI()
        setupScanButtons()
        setupConnectivityListener()   // Écoute le retour de connexion
        checkPendingScansOnStart()    // Vérifie au démarrage
    }

    private fun setupUI() {
        val username = OSMApplication.sessionManager.getUsername()
        binding.tvWelcome.text =
            if (!username.isNullOrBlank()) "Bonjour, $username !" else "Bonjour !"

        binding.cardOF.setOnClickListener { openEntityList(EntityListType.OF) }
        binding.cardColis.setOnClickListener { openEntityList(EntityListType.COLIS) }
        binding.cardPalette.setOnClickListener { openEntityList(EntityListType.PALETTE) }
        binding.cardLot.setOnClickListener { openEntityList(EntityListType.LOT) }
        binding.cardFiltration.setOnClickListener {
            Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupScanButtons() {
        binding.fabStartScan.visibility = View.GONE
        binding.fabScanHistory.visibility = View.GONE

        binding.btnLaunchScan.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabStartScan.setOnClickListener {
            toggleFabMenu()
            startQRScanner()
        }

        binding.fabScanHistory.setOnClickListener {
            toggleFabMenu()
            showScanHistory()
        }
    }

    /**
     * Écouteur de connexion réseau : déclenche la synchronisation dès que la connexion revient.
     */
    private fun setupConnectivityListener() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityListener = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                runOnUiThread {
                    syncPendingScans()
                }
            }
        }

        connectivityListener?.let {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                it
            )
        }
    }

    /**
     * Vérifie s'il y a des scans en attente au démarrage.
     * Si la connexion est disponible, lance la synchronisation.
     */
    private fun checkPendingScansOnStart() {
        if (offlineManager.hasPendingScans()) {
            if (NetworkUtils.isInternetAvailable(this)) {
                syncPendingScans()
            } else {
                Snackbar.make(
                    binding.root,
                    "${offlineManager.getPendingScansCount()} scan(s) en attente de synchronisation",
                    Snackbar.LENGTH_LONG
                ).setAction("Voir") {
                    startActivity(Intent(this, SyncQueueActivity::class.java))
                }.show()
            }
        }
    }

    /**
     * Synchronise tous les scans en attente avec le serveur.
     */
    private fun syncPendingScans() {
        val pendingCount = offlineManager.getPendingScansCount()
        if (pendingCount == 0) return

        val snackbar = Snackbar.make(
            binding.root,
            "Synchronisation de $pendingCount scan(s)...",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.show()

        offlineManager.syncPendingScans { successCount, failedCount ->
            snackbar.dismiss()
            if (failedCount == 0) {
                Snackbar.make(
                    binding.root,
                    "$successCount scan(s) synchronisé(s) avec succès",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                Snackbar.make(
                    binding.root,
                    "$successCount synchronisé(s), $failedCount échoué(s)",
                    Snackbar.LENGTH_LONG
                ).setAction("Voir") {
                    startActivity(Intent(this, SyncQueueActivity::class.java))
                }.show()
            }
        }
    }

    private fun toggleFabMenu() {
        isFabMenuExpanded = !isFabMenuExpanded

        if (isFabMenuExpanded) {
            binding.fabStartScan.visibility = View.VISIBLE
            binding.fabScanHistory.visibility = View.VISIBLE

            binding.fabStartScan.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200)
                .start()

            binding.fabScanHistory.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200)
                .start()

            binding.btnLaunchScan.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            binding.fabStartScan.animate()
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(200)
                .start()

            binding.fabScanHistory.animate()
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.fabStartScan.visibility = View.GONE
                    binding.fabScanHistory.visibility = View.GONE
                }
                .start()

            binding.btnLaunchScan.setImageResource(android.R.drawable.ic_input_add)
        }
    }

    private fun startQRScanner() {
        val intent = Intent(this, QRScannerActivity::class.java)
        scanResultLauncher.launch(intent)
    }

    private fun showScanHistory() {
        val history = scanHistoryManager.getScanHistory()

        if (history.isEmpty()) {
            Snackbar.make(binding.root, "Aucun scan enregistré", Snackbar.LENGTH_SHORT).show()
            return
        }

        ScanHistoryDialog.show(supportFragmentManager, history) { scanItem ->
            handleScanItemClick(scanItem)
        }
    }

    private val scanResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val scannedContent = data.getStringExtra("scanned_content")
                val scanType = data.getStringExtra("scan_type") ?: "QR_CODE"

                if (!scannedContent.isNullOrBlank()) {
                    // Historique local
                    val scanItem = ScanItem(
                        content = scannedContent,
                        type = scanType,
                        timestamp = System.currentTimeMillis()
                    )
                    scanHistoryManager.addScanToHistory(scanItem)

                    // Sauvegarde hors ligne avec ScanData
                    val scanData = ScanData(
                        content = scannedContent,
                        type = scanType,
                        format = "QR_CODE",
                        timestamp = System.currentTimeMillis()
                    )
                    offlineManager.saveScanOffline(scanData)

                    Snackbar.make(
                        binding.root,
                        "Scan réussi: $scannedContent",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Voir") {
                            showScanHistory()
                        }
                        .show()
                }
            }
        }
    }

    private fun handleScanItemClick(scanItem: ScanItem) {
        Toast.makeText(this, "Scan: ${scanItem.content}", Toast.LENGTH_SHORT).show()

        when {
            scanItem.content.startsWith("OF:") -> openEntityList(EntityListType.OF)
            scanItem.content.startsWith("LOT:") -> openEntityList(EntityListType.LOT)
            scanItem.content.startsWith("COLIS:") -> openEntityList(EntityListType.COLIS)
            scanItem.content.startsWith("PALETTE:") -> openEntityList(EntityListType.PALETTE)
            else -> Snackbar.make(binding.root, "Type de scan non reconnu", Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                startActivity(Intent(this, SyncQueueActivity::class.java))
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }

            R.id.action_clear_history -> {
                clearScanHistory()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearScanHistory() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Effacer l'historique")
            .setMessage("Voulez-vous vraiment effacer tout l'historique des scans ?")
            .setPositiveButton("Effacer") { _, _ ->
                scanHistoryManager.clearHistory()
                Snackbar.make(binding.root, "Historique effacé", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun logout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Déconnexion")
            .setMessage("Voulez-vous vraiment vous déconnecter ?")
            .setPositiveButton("Se déconnecter") { _, _ ->
                OSMApplication.sessionManager.clearSession()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun openEntityList(type: EntityListType) {
        startActivity(EntityListActivity.newIntent(this, type))
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityListener?.let {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
    }
}