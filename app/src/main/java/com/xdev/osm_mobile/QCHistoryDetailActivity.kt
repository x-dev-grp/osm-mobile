package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xdev.osm_mobile.databinding.ActivityQchistoryDetailBinding
import com.xdev.osm_mobile.models.QCResultDTO
import com.xdev.osm_mobile.myAdapter.QCHistoryAdapter
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QCHistoryDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQchistoryDetailBinding
    private lateinit var adapter: QCHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQchistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        val ofId = intent.getStringExtra(EXTRA_OF_ID)
        val ofLabel = intent.getStringExtra(EXTRA_OF_LABEL) ?: "OF"

        if (ofId.isNullOrEmpty()) {
            Toast.makeText(this, "ID OF manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.title = "Historique QC - $ofLabel"
        loadHistory(ofId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    private fun loadHistory(ofId: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getQCHistory(ofId)
                }
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    val history: List<QCResultDTO> = apiResponse.data ?: emptyList()
                    if (history.isEmpty()) {
                        Toast.makeText(this@QCHistoryDetailActivity, "Aucun contrôle QC enregistré", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        setupRecyclerView(history)
                    }
                } else {
                    Toast.makeText(this@QCHistoryDetailActivity, "Erreur chargement historique", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@QCHistoryDetailActivity, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    private fun setupRecyclerView(history: List<QCResultDTO>) {
        adapter = QCHistoryAdapter(history)
        binding.rvQCHistory.layoutManager = LinearLayoutManager(this)
        binding.rvQCHistory.adapter = adapter
    }
    companion object {
        private const val EXTRA_OF_ID = "extra_of_id"
        private const val EXTRA_OF_LABEL = "extra_of_label"

        fun newIntent(context: Context, ofId: String, ofLabel: String): Intent =
            Intent(context, QCHistoryDetailActivity::class.java).apply {
                putExtra(EXTRA_OF_ID, ofId)
                putExtra(EXTRA_OF_LABEL, ofLabel)
            }
    }
}
