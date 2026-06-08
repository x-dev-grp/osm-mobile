package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import com.xdev.osm_mobile.databinding.ActivityExpeditionDetailBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.ExpeditionActionRequest
import com.xdev.osm_mobile.models.ExpeditionDto
import com.xdev.osm_mobile.models.OfflineOperation
import com.xdev.osm_mobile.myAdapter.ExpeditionLineAdapter
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ExpeditionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpeditionDetailBinding
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var expedition: ExpeditionDto? = null
    private val gson = Gson()
    private var expeditionId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpeditionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupSwipeRefresh()
        expeditionId = intent.getStringExtra(EXTRA_EXPEDITION_ID) ?: run {
            finish(); return
        }
        loadExpedition()
    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Détail Expédition"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    private fun setupSwipeRefresh() {
        swipeRefresh = binding.swipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                lifecycleScope.launch {
                    try {
                        OSMApplication.repository.refreshExpedition(expeditionId)
                        loadExpedition()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@ExpeditionDetailActivity,
                            "Erreur synchronisation",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        swipeRefresh.isRefreshing = false
                    }
                }
            } else {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Pas de connexion internet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadExpedition() {
        lifecycleScope.launch {
            val cached = withContext(Dispatchers.IO) {
                OSMApplication.repository.findExpeditionById(expeditionId)
            }
            if (cached != null) {
                try {
                    val dto = withContext(Dispatchers.IO) {
                        gson.fromJson(cached.fullJson ?: "{}", ExpeditionDto::class.java)
                    }
                    expedition = dto
                    displayDetails()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (NetworkUtils.isInternetAvailable(this@ExpeditionDetailActivity)) {
                try {
                    OSMApplication.repository.refreshExpedition(expeditionId)
                    val updated = withContext(Dispatchers.IO) {
                        OSMApplication.repository.findExpeditionById(expeditionId)
                    }
                    if (updated != null) {
                        val dto = withContext(Dispatchers.IO) {
                            gson.fromJson(updated.fullJson ?: "{}", ExpeditionDto::class.java)
                        }
                        expedition = dto
                        displayDetails()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    private fun displayDetails() {
        val exp = expedition ?: return
        with(binding) {
            tvExpeditionNumber.text = exp.expeditionNumber ?: "-"
            tvDestination.text = exp.destination ?: "-"
            tvProjet.text = exp.projetCode ?: "-"
            tvPlannedDate.text = exp.plannedShipDate ?: "-"
            tvCarrier.text = exp.carrierName ?: "-"
            tvDriver.text = exp.driverName ?: "-"
            tvTruck.text = exp.truckNumber ?: "-"
            tvTotalQty.text = "${exp.totalQuantity ?: 0}"
            tvStatus.text = exp.status ?: "-"
            val color = when (exp.status?.uppercase()) {
                "DRAFT" -> android.graphics.Color.GRAY
                "READY" -> getColor(R.color.colorAccent)
                "VALIDATED" -> getColor(R.color.colorPrimary)
                "SHIPPED" -> getColor(R.color.olive_green)
                "DELIVERED", "CLOSED" -> android.graphics.Color.DKGRAY
                "CANCELLED" -> android.graphics.Color.RED
                else -> android.graphics.Color.LTGRAY
            }
            tvStatus.setBackgroundColor(color)

            // Bouton "Valider Logistique" (READY → VALIDATED)
            val canValidate = exp.status?.uppercase() == "READY"
            btnValidateExpedition.visibility = if (canValidate) View.VISIBLE else View.GONE
            btnValidateExpedition.setOnClickListener {
                exp.id?.let { expId -> performAction(expId, "validate") }
            }

            // Bouton "Charger l'expédition" (VALIDATED → SHIPPED) avec boîte de confirmation
            val canLoad = exp.status?.uppercase() == "VALIDATED"
            btnScanLoading.visibility = if (canLoad) View.VISIBLE else View.GONE
            if (canLoad) {
                btnScanLoading.text = "Charger l'expédition"
                btnScanLoading.setOnClickListener {
                    exp.id?.let { expId -> showShipConfirmationDialog(expId) }
                }
            }

            // Bouton "Marquer Livrée" (SHIPPED → DELIVERED)
            val canDeliver = exp.status?.uppercase() == "SHIPPED"
            btnDeliverExpedition.visibility = if (canDeliver) View.VISIBLE else View.GONE
            btnDeliverExpedition.setOnClickListener {
                exp.id?.let { expId -> performAction(expId, "deliver") }
            }

            // Bouton "Picking" (DRAFT ou READY)
            val canPick = exp.status?.uppercase() == "DRAFT" || exp.status?.uppercase() == "READY"
            btnStartPicking.visibility = if (canPick) View.VISIBLE else View.GONE
            btnStartPicking.setOnClickListener {
                exp.id?.let { expId ->
                    startActivity(ExpeditionPickingActivity.newIntent(this@ExpeditionDetailActivity, expId))
                }
            }

            rvLines.layoutManager = LinearLayoutManager(this@ExpeditionDetailActivity)
            rvLines.adapter = ExpeditionLineAdapter(exp.lines ?: emptyList())
        }
    }
    private fun showShipConfirmationDialog(expeditionId: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmation de chargement")
            .setMessage("⚠️ Cette action déclenche la sortie définitive des stocks et marque l'expédition comme EXPÉDIÉE (SHIPPED).\n\nSouhaitez-vous continuer ?")
            .setPositiveButton("OK") { _, _ ->
                performAction(expeditionId, "ship")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun performAction(expeditionId: String, action: String) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            val body = gson.toJson(ExpeditionActionRequest(comment = "Action hors ligne: $action"))
            OfflineManager.getInstance(this).saveOperation(
                OfflineOperation(
                    operationId = UUID.randomUUID().toString(),
                    url = "/api/expeditions/$expeditionId/$action",
                    method = "POST",
                    body = body
                )
            )
            Toast.makeText(this, "Action enregistrée hors ligne", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val request = ExpeditionActionRequest(comment = "Action mobile: $action")
                val response = withContext(Dispatchers.IO) {
                    when (action) {
                        "validate" -> RetrofitClient.instance.validateExpedition(expeditionId, request)
                        "deliver"  -> RetrofitClient.instance.deliverExpedition(expeditionId, request)
                        "ship"     -> RetrofitClient.instance.shipExpedition(expeditionId, request)
                        else -> throw IllegalArgumentException("Action inconnue")
                    }
                }
                if (response.isSuccessful) {
                    Toast.makeText(this@ExpeditionDetailActivity, "✅ Opération réussie", Toast.LENGTH_SHORT).show()
                    loadExpedition()
                } else {
                    Toast.makeText(this@ExpeditionDetailActivity, "Erreur ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpeditionDetailActivity, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val EXTRA_EXPEDITION_ID = "extra_expedition_id"
        fun newIntent(context: Context, expeditionId: String) =
            Intent(context, ExpeditionDetailActivity::class.java).apply {
                putExtra(EXTRA_EXPEDITION_ID, expeditionId)
            }
    }
}