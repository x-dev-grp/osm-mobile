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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xdev.osm_mobile.databinding.ActivityMainBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.EntityListType
import com.xdev.osm_mobile.models.OneSignalManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var offlineManager: OfflineManager

    private var connectivityListener: ConnectivityManager.NetworkCallback? = null
    private var mode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        offlineManager = OfflineManager.getInstance(this)

        val user = OSMApplication.sessionManager.getUserId()

        if (user != null) {
            OneSignalManager.login(user)
        }

        setupUI()
        setupScanButtons()
        setupConnectivityListener()
        applyWindowInsets()

        mode = intent.getStringExtra("mode")
    }

    override fun onResume() {
        super.onResume()
        loadRecentSyncItems()
        refreshOverviewCounts()
    }

    private fun refreshOverviewCounts() {
        lifecycleScope.launch {
            try {

                val ofs = OSMApplication.repository.allOfs.firstOrNull()
                val activeOfs = ofs?.count { it.statut == "EN_COURS" } ?: 0
                binding.tvCountOf.text = activeOfs.toString()

                val stocks = OSMApplication.repository.allStocks.firstOrNull()
                binding.tvCountStock.text = (stocks?.size ?: 0).toString()

                val expeditions = OSMApplication.repository.allExpeditions.firstOrNull()
                binding.tvCountExpeditions.text = (expeditions?.size ?: 0).toString()

                binding.tvCountQc.text = "0"

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadRecentSyncItems() {

        val container = binding.llDashboardSyncItems
        container.removeAllViews()

        val ops = offlineManager.getAllOperationsWithStatus()
            .map { com.xdev.osm_mobile.myAdapter.SyncItem.Operation(it) }

        val allItems = ops.sortedByDescending { it.timestamp }.take(3)

        if (allItems.isEmpty()) {
            binding.tvDashboardSyncEmpty.visibility = View.VISIBLE
            return
        }

        binding.tvDashboardSyncEmpty.visibility = View.GONE

        val inflater = android.view.LayoutInflater.from(this)

        for (item in allItems) {

            val view =
                inflater.inflate(R.layout.item_sync_dashboard, container, false)

            val ivIcon =
                view.findViewById<android.widget.ImageView>(R.id.ivIcon)

            val tvContent =
                view.findViewById<android.widget.TextView>(R.id.tvContent)

            val tvStatus =
                view.findViewById<android.widget.TextView>(R.id.tvStatus)

            val rootLayout =
                view.findViewById<android.widget.LinearLayout>(R.id.rootLayout)

            tvContent.text = item.label

            when (item.status) {

                com.xdev.osm_mobile.models.SyncStatus.SYNCED -> {

                    tvStatus.text = "SYNCED"

                    tvStatus.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.olive_green
                        )
                    )

                    tvContent.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.olive_green
                        )
                    )

                    ivIcon.setImageResource(android.R.drawable.presence_online)

                    ivIcon.imageTintList =
                        android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(
                                this,
                                R.color.olive_green
                            )
                        )
                }

                com.xdev.osm_mobile.models.SyncStatus.PENDING -> {

                    tvStatus.text = "PENDING"

                    tvStatus.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.olive_gold
                        )
                    )

                    tvContent.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.olive_gold
                        )
                    )

                    ivIcon.setImageResource(android.R.drawable.ic_popup_sync)
                }

                com.xdev.osm_mobile.models.SyncStatus.ERROR -> {

                    tvStatus.text = "ERROR"

                    tvStatus.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.error
                        )
                    )

                    tvContent.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.error
                        )
                    )

                    ivIcon.setImageResource(android.R.drawable.ic_dialog_info)
                }
            }

            container.addView(view)
        }
    }

    private fun setupUI() {

        val username = OSMApplication.sessionManager.getUsername()

        binding.tvWelcome.text =
            if (!username.isNullOrBlank())
                "Bonjour, $username"
            else
                "Bonjour"

        binding.cardMainScanner.setOnClickListener {
            startQRScanner()
        }

        binding.cardManualEntry.setOnClickListener {
            startActivity(ManualCodeEntryActivity.newIntent(this))
        }

        binding.cardOverviewOf.setOnClickListener {
            startActivity(EntityListActivity.newIntent(this))
        }

        binding.cardOverviewQc.setOnClickListener {

            val intent = Intent(this, QRScannerActivity::class.java)
            intent.putExtra("mode", "qc_direct")

            startActivity(intent)
        }

        binding.cardOverviewStock.setOnClickListener {
            startActivity(AllStockListActivity.newIntent(this))
        }

        binding.cardOverviewExpeditions.setOnClickListener {
            startActivity(ExpeditionListActivity.newIntent(this))
        }

        binding.layoutSyncQueue.setOnClickListener {
            startActivity(Intent(this, SyncQueueActivity::class.java))
        }
    }

    private fun setupScanButtons() {

        binding.fabStartScan.visibility = View.GONE
        binding.fabScanHistory.visibility = View.GONE
        binding.fabManualEntry.visibility = View.GONE
        binding.btnLaunchScan.visibility = View.GONE

        binding.navHome.setOnClickListener {}

        binding.navScan.setOnClickListener {
            startQRScanner()
        }

        binding.navQuality.setOnClickListener {

            val intent = Intent(this, QRScannerActivity::class.java)
            intent.putExtra("mode", "history")

            startActivity(intent)
        }

        binding.navStock.setOnClickListener {
            startActivity(StockMovementListActivity.newIntent(this))
        }

        binding.navProfile.setOnClickListener {
            logout()
        }
    }

    private fun applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.bottomNavBar.setPadding(
                binding.bottomNavBar.paddingLeft,
                binding.bottomNavBar.paddingTop,
                binding.bottomNavBar.paddingRight,
                systemBars.bottom +
                        resources.getDimensionPixelSize(
                            R.dimen.nav_bar_padding_bottom
                        )
            )

            insets
        }
    }

    private fun setupConnectivityListener() {

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        connectivityListener =
            object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)

                    runOnUiThread {

                        Log.d(
                            "Network",
                            "Connexion rétablie, synchronisation..."
                        )

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

        if (!offlineManager.hasPendingOperations()) return

        val snackbar = Snackbar.make(
            binding.root,
            "Synchronisation en cours...",
            Snackbar.LENGTH_INDEFINITE
        )

        snackbar.show()

        offlineManager.syncPendingOperations { successOps, failedOps ->

            snackbar.dismiss()

            if (failedOps == 0) {

                Snackbar.make(
                    binding.root,
                    "$successOps élément(s) synchronisé(s)",
                    Snackbar.LENGTH_LONG
                ).show()

            } else {

                Snackbar.make(
                    binding.root,
                    "$successOps synchronisé(s), $failedOps échoué(s)",
                    Snackbar.LENGTH_LONG
                ).setAction("Voir") {

                    startActivity(
                        Intent(this, SyncQueueActivity::class.java)
                    )

                }.show()
            }
        }
    }

    private fun startQRScanner() {

        val intent = Intent(this, QRScannerActivity::class.java)

        scanResultLauncher.launch(intent)
    }

    private val scanResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK) {

                result.data?.let { data ->

                    val scannedContent =
                        data.getStringExtra("scanned_content")

                    if (!scannedContent.isNullOrBlank()) {

                        handleScanDirect(scannedContent)

                        Snackbar.make(
                            binding.root,
                            "Scan réussi: $scannedContent",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    private fun handleScanDirect(content: String) {

        val repository = OSMApplication.repository

        lifecycleScope.launch {

            when {

                content.startsWith("OF:") || content.contains("-") -> {

                    val code = content.removePrefix("OF:")

                    val cachedOf =
                        repository.findOfByCode(code)

                    if (cachedOf != null) {

                        startActivity(
                            OfDetailActivity.newIntent(
                                this@MainActivity,
                                cachedOf.id
                            )
                        )

                    } else {

                        startActivity(
                            EntityListActivity.newIntent(this@MainActivity)
                        )
                    }
                }

                content.startsWith("LOT:") -> {

                    val number = content.removePrefix("LOT:")

                    val cachedLot =
                        repository.findLotByNumber(number)

                    if (cachedLot != null) {

                        Toast.makeText(
                            this@MainActivity,
                            "Lot trouvé: ${cachedLot.lotNumber}",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this@MainActivity,
                            "Lot non trouvé",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                content.startsWith("ART:") || !content.contains(":") -> {

                    val code = content.removePrefix("ART:")

                    var cachedArticle =
                        repository.findArticleByQrHex(code)

                    if (cachedArticle == null) {
                        cachedArticle =
                            repository.getArticleById(code)
                    }

                    if (cachedArticle != null) {

                        startActivity(
                            ArticleDetailActivity.newIntent(
                                this@MainActivity,
                                cachedArticle.id
                            )
                        )

                    } else {

                        Toast.makeText(
                            this@MainActivity,
                            "Article non trouvé: $code",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                else -> {

                    Toast.makeText(
                        this@MainActivity,
                        "Scan: $content",
                        Toast.LENGTH_SHORT
                    ).show()
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

                startActivity(
                    Intent(this, SyncQueueActivity::class.java)
                )

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

                startActivity(
                    Intent(this, LoginActivity::class.java).apply {

                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )

                finish()
            }

            .setNegativeButton("Annuler", null)
            .show()
    }

    @Deprecated("Plus utilisée")
    private fun openEntityList(type: EntityListType) {

        if (type == EntityListType.OF) {

            startActivity(EntityListActivity.newIntent(this))

        } else {

            Toast.makeText(
                this,
                "Fonctionnalité en développement",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        connectivityListener?.let {

            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE)
                        as ConnectivityManager

            connectivityManager.unregisterNetworkCallback(it)
        }
    }
}