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
                return@setOnClickListener
            }
            resolveCode(code)
        }
    }

    private fun resolveCode(code: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnResolve.isEnabled = false

        lifecycleScope.launch {
            val repository = OSMApplication.repository
            val cachedOf = withContext(Dispatchers.IO) {
                repository.findOfByQrHex(code) ?: repository.findOfByCode(code)
            }
            if (cachedOf != null) {
                handleResolvedEntity(QrResolveResponse("OF", code, cachedOf.id, cachedOf.code ?: "", "OK", ""))
                return@launch
            }

            var cachedArticle = withContext(Dispatchers.IO) { repository.findArticleByQrHex(code) }
            if (cachedArticle == null) {
                cachedArticle = withContext(Dispatchers.IO) { repository.getArticleById(code) }
            }
            if (cachedArticle != null) {
                handleResolvedEntity(QrResolveResponse("ARTICLE", code, cachedArticle.id, cachedArticle.nom ?: "", "OK", "/article/detail"))
                return@launch
            }
            if (!NetworkUtils.isInternetAvailable(this@ManualCodeEntryActivity)) {
                binding.progressBar.visibility = View.GONE
                binding.btnResolve.isEnabled = true
                Toast.makeText(this@ManualCodeEntryActivity, "Code inconnu et pas de connexion", Toast.LENGTH_LONG).show()
                return@launch
            }

            try {
                val apiCode = if (code.length > 6) code.take(6) else code
                var response = withContext(Dispatchers.IO) { RetrofitClient.instance.resolveOF(apiCode) }
                if (response.isSuccessful && response.body() != null) {
                    handleResolvedEntity(response.body()!!)
                    return@launch
                }
                response = withContext(Dispatchers.IO) { RetrofitClient.instance.resolveArticle(apiCode) }
                if (response.isSuccessful && response.body() != null) {
                    handleResolvedEntity(response.body()!!)
                    return@launch
                }
                binding.progressBar.visibility = View.GONE
                binding.btnResolve.isEnabled = true
                Toast.makeText(this@ManualCodeEntryActivity, "Non trouvé sur le serveur ($apiCode)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnResolve.isEnabled = true
                Toast.makeText(this@ManualCodeEntryActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleResolvedEntity(response: QrResolveResponse) {
        when (response.entityType) {
            "OF" -> {
                startActivity(OfDetailActivity.newIntent(this, response.entityId))
                finish()
            }
            "ARTICLE" -> {
                startActivity(ArticleDetailActivity.newIntent(this, response.entityId))
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