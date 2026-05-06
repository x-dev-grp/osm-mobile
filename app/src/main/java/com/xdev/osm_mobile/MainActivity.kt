package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xdev.osm_mobile.databinding.ActivityMainBinding
import com.xdev.osm_mobile.models.ScanData
import com.xdev.osm_mobile.models.EntityListType



import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.horsligne.SessionManager
import com.xdev.osm_mobile.models.OneSignalManager
import com.xdev.osm_mobile.scan.ScanHistoryDialog
import com.xdev.osm_mobile.scan.ScanHistoryManager
import com.xdev.osm_mobile.scan.ScanItem
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var scanHistoryManager: ScanHistoryManager
    private lateinit var offlineManager: OfflineManager
    private lateinit var sessionManager: SessionManager
    private var isFabMenuExpanded = false
    private var connectivityListener: ConnectivityManager.NetworkCallback? = null
    private var mode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        scanHistoryManager = ScanHistoryManager(this)
        offlineManager = OfflineManager.getInstance(this)
        val user = OSMApplication.sessionManager.getUserId()

        if (user != null) {
            OneSignalManager.login(user)
        };
        setupUI()
        setupScanButtons()
        setupConnectivityListener()
        checkPendingScansOnStart()

        mode = intent.getStringExtra("mode")
    }
    private fun setupUI() {
        val username = OSMApplication.sessionManager.getUsername()
        binding.tvWelcome.text =
            if (!username.isNullOrBlank()) "Bonjour, $username !" else "Bonjour !"

        binding.cardOF.setOnClickListener { openEntityList(EntityListType.OF) }

        binding.cardStockMovement.setOnClickListener {
            startActivity(StockMovementScannerActivity.newIntent(this))
        }
        binding.cardQCHistory.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java).apply {
                putExtra("mode", "history")  // ← mode historique
            }
            startActivity(intent)
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
        binding.fabManualEntry.setOnClickListener {     // <- nouveau
            toggleFabMenu()
            startActivity(ManualCodeEntryActivity.newIntent(this))
        }


    }
    private fun setupConnectivityListener() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityListener = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                runOnUiThread {
                    Log.d("Network", "Connexion rétablie, synchronisation...")
                    syncAllPending()
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
    private fun syncAllPending() {
        val hasScans = offlineManager.hasPendingScans()
        val hasOps = offlineManager.hasPendingOperations()
        Log.d("Sync", "Scans en attente: $hasScans, Opérations en attente: $hasOps")

        if (!hasScans && !hasOps) return
        val snackbar = Snackbar.make(
            binding.root,
            "Synchronisation en cours...",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.show()
        offlineManager.syncPendingScans { successScans, failedScans ->
            offlineManager.syncPendingOperations { successOps, failedOps ->
                snackbar.dismiss()
                val totalSuccess = successScans + successOps
                val totalFailed = failedScans + failedOps
                if (totalFailed == 0) {
                    Snackbar.make(
                        binding.root,
                        "$totalSuccess élément(s) synchronisé(s)",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(
                        binding.root,
                        "$totalSuccess synchronisé(s), $totalFailed échoué(s)",
                        Snackbar.LENGTH_LONG
                    ).setAction("Voir") {
                        startActivity(Intent(this, SyncQueueActivity::class.java))
                    }.show()
                }
            }
        }
    }
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
            binding.fabManualEntry.visibility = View.VISIBLE

            binding.fabStartScan.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(200).start()

            binding.fabScanHistory.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(200).start()

            binding.fabManualEntry.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(200).start()

            binding.btnLaunchScan.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            binding.fabStartScan.animate()
                .scaleX(0f).scaleY(0f).alpha(0f)
                .setDuration(200).start()

            binding.fabScanHistory.animate()
                .scaleX(0f).scaleY(0f).alpha(0f)
                .setDuration(200).start()

            binding.fabManualEntry.animate()
                .scaleX(0f).scaleY(0f).alpha(0f)
                .setDuration(200)
                .withEndAction {
                    // Cacher les trois FABs une fois l’animation terminée
                    binding.fabStartScan.visibility = View.GONE
                    binding.fabScanHistory.visibility = View.GONE
                    binding.fabManualEntry.visibility = View.GONE
                }.start()

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
                    val scanItem = ScanItem(
                        content = scannedContent,
                        type = scanType,
                        timestamp = System.currentTimeMillis()
                    )
                    scanHistoryManager.addScanToHistory(scanItem)
                    val scanData = ScanData(
                        content = scannedContent,
                        type = scanType,
                        format = "QR_CODE",
                        timestamp = System.currentTimeMillis()
                    )
                    offlineManager.saveScanOffline(scanData)
                    handleScanItemClick(scanItem)

                    Snackbar.make(
                        binding.root,
                        "Scan réussi: $scannedContent",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Historique") {
                            showScanHistory()
                        }
                        .show()
                }
            }
        }
    }

    private fun handleScanItemClick(scanItem: ScanItem) {
        val repository = OSMApplication.repository
        
        lifecycleScope.launch {
            val content = scanItem.content
            
            when {
                content.startsWith("OF:") || content.contains("-") -> {
                    val code = content.removePrefix("OF:")
                    val cachedOf = repository.findOfByCode(code)
                    if (cachedOf != null) {
                        startActivity(OfDetailActivity.newIntent(this@MainActivity, cachedOf.id))
                    } else {
                        openEntityList(EntityListType.OF)
                    }
                }
                content.startsWith("LOT:") -> {
                    val number = content.removePrefix("LOT:")
                    val cachedLot = repository.findLotByNumber(number)
                    if (cachedLot != null) {
                        Toast.makeText(this@MainActivity, "Lot trouvé: ${cachedLot.lotNumber}", Toast.LENGTH_SHORT).show()
                        openEntityList(EntityListType.LOT)
                    } else {
                        openEntityList(EntityListType.LOT)
                    }
                }
                content.startsWith("ART:") || !content.contains(":") -> {
                    val code = content.removePrefix("ART:")
                    var cachedArticle = repository.findArticleByQrHex(code)
                    if (cachedArticle == null) {
                        // Fallback : recherche par ID si qrHex est null
                        cachedArticle = repository.getArticleById(code)
                    }

                    if (cachedArticle != null) {
                        startActivity(ArticleDetailActivity.newIntent(this@MainActivity, cachedArticle.id))
                    } else {
                        Toast.makeText(this@MainActivity, "Article non trouvé: $code", Toast.LENGTH_SHORT).show()
                    }
                }
                
                else -> {
                    Toast.makeText(this@MainActivity, "Scan: $content", Toast.LENGTH_SHORT).show()
                    if (content.startsWith("COLIS:")) openEntityList(EntityListType.COLIS)
                    else if (content.startsWith("PALETTE:")) openEntityList(EntityListType.PALETTE)
                    else Snackbar.make(binding.root, "Type de scan non reconnu", Snackbar.LENGTH_SHORT).show()
                }
            }
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
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun logout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Déconnexion")
            .setMessage("Voulez-vous vraiment vous déconnecter ?")
            .setPositiveButton("Se déconnecter") { _, _ ->
                OSMApplication.sessionManager.clearSession()
                OneSignalManager.logout()
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
