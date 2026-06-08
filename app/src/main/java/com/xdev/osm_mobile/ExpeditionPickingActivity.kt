package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.xdev.osm_mobile.databinding.ActivityExpeditionPickingBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.ExpeditionActionRequest
import com.xdev.osm_mobile.models.ExpeditionDto
import com.xdev.osm_mobile.models.OfflineOperation
import com.xdev.osm_mobile.myAdapter.ExpeditionLineFifoAdapter
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ExpeditionPickingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpeditionPickingBinding
    private var expedition: ExpeditionDto? = null
    private val gson = Gson()
    private val TAG = "ExpeditionPicking"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpeditionPickingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Picking expédition"

        val expeditionId = intent.getStringExtra(EXTRA_EXPEDITION_ID) ?: run { finish(); return }
        loadExpedition(expeditionId)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadExpedition(expeditionId: String) {
        lifecycleScope.launch {
            val repo = OSMApplication.repository

            val cached = withContext(Dispatchers.IO) { repo.findExpeditionById(expeditionId) }
            if (cached != null) {
                try {
                    val cachedDto = withContext(Dispatchers.IO) {
                        gson.fromJson(cached.fullJson ?: "{}", ExpeditionDto::class.java)
                    }
                    expedition = cachedDto
                    updateUI()
                } catch (e: Exception) {
                    Log.e(TAG, "Cache JSON corrompu", e)
                }
            }

            if (NetworkUtils.isInternetAvailable(this@ExpeditionPickingActivity)) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.getExpeditionById(expeditionId)
                    }
                    if (response.isSuccessful && response.body() != null) {
                        expedition = response.body()
                        updateUI()
                        launch(Dispatchers.IO) { repo.refreshExpedition(expeditionId) }
                    } else if (expedition == null) {
                        Toast.makeText(this@ExpeditionPickingActivity, "Expédition non trouvée", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    if (expedition == null) {
                        Toast.makeText(this@ExpeditionPickingActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } else if (expedition == null) {
                Toast.makeText(this@ExpeditionPickingActivity, "Données non disponibles hors ligne", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun displayExpedition() {
        val exp = expedition ?: return
        binding.tvExpeditionNumber.text = exp.expeditionNumber ?: "-"
        binding.tvDestination.text = exp.destination ?: "-"
        binding.tvTotalQty.text = "Total: ${exp.totalQuantity ?: 0} u"
        binding.rvLines.layoutManager = LinearLayoutManager(this)
        binding.rvLines.adapter = ExpeditionLineFifoAdapter(exp.lines ?: emptyList())
    }

    private fun handleStatus() {
        val exp = expedition ?: return
        val statusRaw = exp.status
        val statusUpper = statusRaw?.uppercase(Locale.getDefault())
        Log.d(TAG, "handleStatus: status = $statusUpper")

        when {
            statusUpper == "READY" -> {
                showMessage(
                    "✅ Picking déjà validé — expédition en attente de validation logistique.",
                    "#D4EDDA", "#155724"
                )
                disablePickButton()
            }
            statusUpper == "DRAFT" -> {
                showMessage("✅ Picking possible. Veuillez confirmer la préparation.", "#D4EDDA", "#155724")
                enablePickButton()
            }
            else -> {
                showMessage(
                    "⚠️ Statut actuel : $statusRaw. Le picking n'est pas autorisé.",
                    "#F8D7DA", "#721C24"
                )
                disablePickButton()
            }
        }
    }

    private fun updateUI() {
        val exp = expedition ?: return
        displayExpedition()
        handleStatus()
    }

    private fun showMessage(text: String, bgColorHex: String, textColorHex: String) {
        binding.fifoWarning.visibility = View.VISIBLE
        binding.fifoWarning.setBackgroundColor(Color.parseColor(bgColorHex))
        binding.fifoWarning.setTextColor(Color.parseColor(textColorHex))
        binding.fifoWarning.text = text
    }

    private fun disablePickButton() {
        binding.btnConfirmPicking.isEnabled = false
        binding.btnConfirmPicking.alpha = 0.4f
    }

    private fun enablePickButton() {
        binding.btnConfirmPicking.isEnabled = true
        binding.btnConfirmPicking.alpha = 1f
        binding.btnConfirmPicking.setOnClickListener {
            showPickingConfirmationDialog()
        }
    }

    private fun showPickingConfirmationDialog() {
        val exp = expedition ?: return
        val linesText = exp.lines?.joinToString("\n") { line ->
            "• ${line.articleName ?: "Article"} — ${line.quantity} ${line.unit ?: ""}" +
                    if (!line.lotNumber.isNullOrBlank()) " | Lot: ${line.lotNumber}" else ""
        } ?: "Aucune ligne"

        AlertDialog.Builder(this)
            .setTitle("Confirmer le picking")
            .setMessage("Articles préparés :\n$linesText\n\nConfirmez-vous le picking ?")
            .setPositiveButton("Confirmer") { _, _ -> confirmPicking() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmPicking() {
        val exp = expedition ?: return
        val expeditionId = exp.id ?: return

        if (!NetworkUtils.isInternetAvailable(this)) {
            saveOfflineReady(expeditionId)
            return
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.markExpeditionReady(
                        expeditionId,
                        ExpeditionActionRequest(comment = "Picking validé mobile")
                    )
                }
                if (response.isSuccessful) {
                    Toast.makeText(this@ExpeditionPickingActivity, "✅ Expédition marquée READY", Toast.LENGTH_SHORT).show()
                    launch(Dispatchers.IO) { OSMApplication.repository.refreshExpedition(expeditionId) }
                    finish()
                } else {
                    Toast.makeText(this@ExpeditionPickingActivity, "Erreur ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpeditionPickingActivity, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveOfflineReady(expeditionId: String) {
        val body = gson.toJson(ExpeditionActionRequest(comment = "Picking hors ligne"))
        OfflineManager.getInstance(this).saveOperation(
            OfflineOperation(
                operationId = UUID.randomUUID().toString(),
                url = "/api/expeditions/$expeditionId/ready",
                method = "POST",
                body = body
            )
        )
        Toast.makeText(this, "Enregistré hors ligne, sera synchronisé plus tard", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val EXTRA_EXPEDITION_ID = "extra_expedition_id"
        fun newIntent(context: Context, expeditionId: String) =
            Intent(context, ExpeditionPickingActivity::class.java).apply {
                putExtra(EXTRA_EXPEDITION_ID, expeditionId)
            }
    }
}