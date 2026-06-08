package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.xdev.osm_mobile.databinding.ActivityManualCodeEntryBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.models.QrResolveResponse
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManualCodeEntryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManualCodeEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualCodeEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar?.title = "Saisie manuelle du code"
    }
    private fun setupListeners() {
        binding.btnResolve.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Veuillez saisir un code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener//si aucun code afficher le message puis aretter
            }
            resolveCode(code)
        }
    }
    private fun resolveCode(code: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnResolve.isEnabled = false
        lifecycleScope.launch {
            val repository = OSMApplication.repository
            if (NetworkUtils.isInternetAvailable(this@ManualCodeEntryActivity)) {
                try {
                    val apiCode = if (code.length > 6) code.take(6) else code
                    var response = withContext(Dispatchers.IO) { RetrofitClient.instance.resolveOF(apiCode) }
                    if (response.isSuccessful && response.body() != null) {
                        val entity = response.body()!!
                        withContext(Dispatchers.IO) { repository.refreshOfDetail(entity.entityId) }
                        handleResolvedEntity(entity)
                        return@launch
                    }
                    response = withContext(Dispatchers.IO) { RetrofitClient.instance.resolveArticle(apiCode) }
                    if (response.isSuccessful && response.body() != null) {
                        val entity = response.body()!!
                        withContext(Dispatchers.IO) { repository.refreshArticle(entity.entityId) }
                        handleResolvedEntity(entity)
                        return@launch
                    }
                    response = withContext(Dispatchers.IO) { RetrofitClient.instance.resolveExpedition(apiCode) }
                    if (response.isSuccessful && response.body() != null) {
                        val entity = response.body()!!
                        withContext(Dispatchers.IO) { repository.refreshExpedition(entity.entityId) }
                        handleResolvedEntity(entity)
                        return@launch
                    }
                    showError("Code non trouvé sur le serveur : $apiCode")
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("Erreur réseau : ${e.message}")
                }
            } else {
                var cachedOf = withContext(Dispatchers.IO) {
                    repository.findOfByQrHex(code) ?: repository.findOfByCode(code)
                }
                if (cachedOf != null) {
                    handleResolvedEntity(
                        QrResolveResponse(
                            entityType = "OF",
                            publicCode = code,
                            entityId = cachedOf.id,
                            label = cachedOf.code ?: "",
                            status = "OK",
                            mobileRoute = ""
                        )
                    )
                    return@launch
                }
                var cachedArticle = withContext(Dispatchers.IO) { repository.findArticleByQrHex(code) }
                if (cachedArticle == null) {
                    cachedArticle = withContext(Dispatchers.IO) { repository.getArticleById(code) }
                }
                if (cachedArticle != null) {
                    handleResolvedEntity(
                        QrResolveResponse(
                            entityType = "ARTICLE",
                            publicCode = code,
                            entityId = cachedArticle.id,
                            label = cachedArticle.nom ?: "",
                            status = "OK",
                            mobileRoute = ""
                        )
                    )
                    return@launch
                }

                val cachedExpedition = withContext(Dispatchers.IO) {
                    repository.findExpeditionByPublicCode(code)
                }
                if (cachedExpedition != null) {
                    handleResolvedEntity(
                        QrResolveResponse(
                            entityType = "EXPEDITION",
                            publicCode = code,
                            entityId = cachedExpedition.id,
                            label = cachedExpedition.expeditionNumber ?: "",
                            status = cachedExpedition.status ?: "OK",
                            mobileRoute = ""
                        )
                    )
                    return@launch
                }
                showError("Code inconnu et mode hors ligne")
            }
            binding.progressBar.visibility = View.GONE
            binding.btnResolve.isEnabled = true
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnResolve.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun handleResolvedEntity(response: QrResolveResponse) {
        binding.progressBar.visibility = View.GONE
        binding.btnResolve.isEnabled = true

        when (response.entityType) {
            "OF" -> {
                startActivity(OfDetailActivity.newIntent(this, response.entityId))
                finish()
            }
            "ARTICLE" -> {
                startActivity(ArticleDetailActivity.newIntent(this, response.entityId))
                finish()
            }
            "EXPEDITION" -> {
                startActivity(ExpeditionDetailActivity.newIntent(this, response.entityId))
                finish()
            }
            else -> {
                Toast.makeText(this, "Type d'entité non supporté: ${response.entityType}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    companion object {
        fun newIntent(context: Context): Intent = Intent(context, ManualCodeEntryActivity::class.java)
    }
}