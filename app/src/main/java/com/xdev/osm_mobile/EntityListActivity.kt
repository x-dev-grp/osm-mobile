package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xdev.osm_mobile.databinding.ActivityEntityListBinding
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.models.OrderFabricationDTO
import com.xdev.osm_mobile.myAdapter.EntityAdapter
import com.xdev.osm_mobile.myAdapter.EntityItem
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntityListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntityListBinding
    private lateinit var adapter: EntityAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var currentFilter: String = "TOUS"
    private var searchQuery: String = ""
    private var allItems: List<EntityItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntityListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        initRecyclerView()
        setupSwipeRefresh()
        setupSearchAndFilters()
        observeData()
        lifecycleScope.launch {
            if (NetworkUtils.isInternetAvailable(this@EntityListActivity)) {
                refreshFromBackend(showToast = false)
            } else {
                Toast.makeText(this@EntityListActivity, "Mode hors ligne - données en cache", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.title = "Ordres de fabrication"
        binding.tvListTitle.visibility = View.GONE
    }
    private fun initRecyclerView() {
        adapter = EntityAdapter(onItemClick = ::handleItemClick)
        binding.rvEntities.layoutManager = LinearLayoutManager(this)
        binding.rvEntities.adapter = adapter
    }
    private fun setupSwipeRefresh() {
        swipeRefresh = binding.swipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                lifecycleScope.launch {
                    refreshFromBackend(showToast = true)
                    swipeRefresh.isRefreshing = false
                }
            } else {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Pas de connexion internet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun refreshFromBackend(showToast: Boolean) {
        try {
            withContext(Dispatchers.IO) {
                val response = RetrofitClient.instance.getOfs()
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("EntityList", "Réponse API OK, taille: ${body?.size ?: 0}")
                    body?.forEach { dto: OrderFabricationDTO ->
                        Log.d("EntityList", "OF: ${dto.code} - statut: ${dto.statut}")
                    }
                } else {
                    Log.e("EntityList", "Erreur API: ${response.code()} - ${response.message()}")
                }
                OSMApplication.repository.refreshOfs()
                Log.d("EntityList", "Base locale mise à jour (refreshOfs exécuté)")
            }
            if (showToast) {
                Toast.makeText(this@EntityListActivity, "Synchronisation réussie", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("EntityList", "Erreur refresh", e)
            if (showToast) {
                Toast.makeText(this@EntityListActivity, "Erreur de synchronisation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.lowercase() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.filtersScroll.visibility = View.VISIBLE
        binding.filterAll.setOnClickListener { setFilter("TOUS") }
        binding.filterInProgress.setOnClickListener { setFilter("EN_COURS") }
        binding.filterPlanned.setOnClickListener { setFilter("PLANIFIE") }
        binding.filterSuspended.setOnClickListener { setFilter("EN_PAUSE") }
        updateFilterButtonsUI()
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        updateFilterButtonsUI()
        applyFilters()
    }

    private fun updateFilterButtonsUI() {
        val activeColor = ContextCompat.getColor(this, R.color.abiooc_primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.gray)
        val activeBg = ContextCompat.getColor(this, R.color.status_in_progress_bg)
        val transparentColor = ContextCompat.getColor(this, android.R.color.transparent)

        binding.filterAll.apply {
            strokeColor = ColorStateList.valueOf(if (currentFilter == "TOUS") activeColor else inactiveColor)
            backgroundTintList = ColorStateList.valueOf(if (currentFilter == "TOUS") activeBg else transparentColor)
        }
        binding.filterInProgress.apply {
            strokeColor = ColorStateList.valueOf(if (currentFilter == "EN_COURS") activeColor else inactiveColor)
            backgroundTintList = ColorStateList.valueOf(if (currentFilter == "EN_COURS") activeBg else transparentColor)
        }
        binding.filterPlanned.apply {
            strokeColor = ColorStateList.valueOf(if (currentFilter == "PLANIFIE") activeColor else inactiveColor)
            backgroundTintList = ColorStateList.valueOf(if (currentFilter == "PLANIFIE") activeBg else transparentColor)
        }
        binding.filterSuspended.apply {
            strokeColor = ColorStateList.valueOf(if (currentFilter == "EN_PAUSE") activeColor else inactiveColor)
            backgroundTintList = ColorStateList.valueOf(if (currentFilter == "EN_PAUSE") activeBg else transparentColor)
        }
    }
   private fun observeData() {
        lifecycleScope.launch {
            OSMApplication.repository.allOfs.collectLatest { entities ->
                Log.d("EntityList", "Collecte entities depuis Room: ${entities.size}")
                allItems = entities.mapNotNull { entity ->
                    try {
                        EntityItem.Of(convertToDto(entity))
                    } catch (e: Exception) {
                        Log.e("EntityList", "Erreur conversion OF ${entity.id}", e)
                        null
                    }
                }
                Log.d("EntityList", "Items convertis: ${allItems.size}")
                applyFilters()
            }
        }
    }

    private fun convertToDto(entity: com.xdev.osm_mobile.database.entities.OfEntity): OrderFabricationDTO {
        return OrderFabricationDTO(
            id = entity.id,
            code = entity.code,
            statut = entity.statut,
            dateDebutPrevue = entity.dateDebutPrevue,
            dateFinPrevue = entity.dateFinPrevue,
            dateDebutReelle = entity.dateDebutReelle,
            dateFinReelle = entity.dateFinReelle,
            quantiteCible = entity.quantiteCible,
            quantiteBonne = entity.quantiteBonne,
            quantiteNC = entity.quantiteNC,
            quantiteDefectueuse = null,
            dureeReelle = null,
            skuId = entity.skuId,
            skuCode = entity.skuCode,
            ligneId = entity.ligneId,
            lotVracNom  = entity.ligneNom,
            lotVracId = entity.lotVracId,
            bomId = entity.bomId,
            sku = null,
            ligneNom = entity.ligneNom,
            lignes = null
        )
    }

    private fun applyFilters() {
        var filtered = allItems

        if (currentFilter != "TOUS") {
            filtered = filtered.filter { item ->
                if (item is EntityItem.Of) {
                    val status = item.data.statut?.uppercase() ?: ""
                    when (currentFilter) {
                        "EN_COURS" -> status == "EN_COURS"
                        "PLANIFIE" -> status == "PLANIFIE"
                        "EN_PAUSE" -> status == "EN_PAUSE"
                        else -> true
                    }
                } else false
            }
        }

        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { item ->
                if (item is EntityItem.Of) {
                    item.data.code?.lowercase()?.contains(searchQuery) == true ||
                            item.data.skuCode?.lowercase()?.contains(searchQuery) == true ||
                            item.data.lotVracId?.lowercase()?.contains(searchQuery) == true
                } else false
            }
        }

        adapter.updateData(filtered)
        Log.d("EntityList", "Après filtres: ${filtered.size} éléments affichés")

        if (filtered.isEmpty()) {
            binding.rvEntities.visibility = View.GONE
            binding.tvListTitle.visibility = View.VISIBLE
            binding.tvListTitle.text = "Aucun ordre de fabrication trouvé"
        } else {
            binding.rvEntities.visibility = View.VISIBLE
            binding.tvListTitle.visibility = View.GONE
        }
    }

    private fun handleItemClick(item: EntityItem) {
        if (item is EntityItem.Of) {
            val ofId = item.data.id
            if (!ofId.isNullOrEmpty()) {
                startActivity(OfDetailActivity.newIntent(this, ofId))
            } else {
                Toast.makeText(this, "ID OF invalide", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, EntityListActivity::class.java)
    }
}