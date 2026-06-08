package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xdev.osm_mobile.database.dao.MainDao
import com.xdev.osm_mobile.databinding.ActivityAllStockListBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.myAdapter.AllStockAdapter
import kotlinx.coroutines.launch

class AllStockListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllStockListBinding
    private lateinit var adapter: AllStockAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var allStocks = listOf<MainDao.StockWithArticle>()
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllStockListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupHeader()
        setupSearch()
        setupSwipeRefresh()
        observeStocks()
        if (NetworkUtils.isInternetAvailable(this)) {
            syncStocksFromBackend(showLoading = true)
        } else {
            showOfflineMessage()
        }
    }
    private fun observeStocks() {
        lifecycleScope.launch {
            OSMApplication.repository.getAllStocksWithArticle().collect { stocks ->
                allStocks = stocks
                applyFilters()
                binding.tvEmpty.visibility = if (stocks.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh = binding.swipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                syncStocksFromBackend(showLoading = false)
            } else {
                swipeRefresh.isRefreshing = false
                showOfflineMessage()
            }
        }
    }
    private fun syncStocksFromBackend(showLoading: Boolean) {
        if (showLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rvStocks.visibility = View.GONE
        }
        lifecycleScope.launch {
            try {
                OSMApplication.repository.refreshAllStocks()
                hideOfflineMessage()
            } catch (e: Exception) {
                e.printStackTrace()
                showErrorMessage("Erreur de synchronisation du cache local")
            } finally {
                if (showLoading) {
                    binding.progressBar.visibility = View.GONE
                    binding.rvStocks.visibility = View.VISIBLE
                }
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showOfflineMessage() {
        binding.layoutOfflineWarning.visibility = View.VISIBLE
        binding.tvOfflineMessage.text = "Mode hors ligne - données en cache"
    }
    private fun hideOfflineMessage() {
        binding.layoutOfflineWarning.visibility = View.GONE
    }

    private fun showErrorMessage(msg: String) {
        binding.layoutOfflineWarning.visibility = View.VISIBLE
        binding.tvOfflineMessage.text = msg
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnInventory.setOnClickListener {
            startActivity(StockMovementScannerActivity.newIntent(this))
        }
    }

    private fun setupRecyclerView() {
        adapter = AllStockAdapter { articleId ->
            Log.d("AllStockList", "Article cliqué : $articleId")
            if (articleId.isNotBlank()) {
                startActivity(ArticleDetailActivity.newIntent(this, articleId))
            } else {
                Toast.makeText(this, "Identifiant d'article invalide", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvStocks.layoutManager = LinearLayoutManager(this)
        binding.rvStocks.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyFilters() {
        val filtered = allStocks.filter { stock ->
            stock.articleName?.contains(searchQuery, ignoreCase = true) == true
        }
        adapter.submitList(filtered)
        val critical = filtered.find { (it.quantiteActuelle ?: 0) <= (it.articleStockMin ?: 0) }
        if (critical != null) {
            binding.layoutAlert.visibility = View.VISIBLE
            binding.tvAlertTitle.text = "${critical.articleName} — stock critique"
            binding.tvAlertBody.text = "Seuil minimal atteint (${critical.articleStockMin}). Commande urgente recommandée."
        } else {
            binding.layoutAlert.visibility = View.GONE
        }

        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, AllStockListActivity::class.java)
    }
}