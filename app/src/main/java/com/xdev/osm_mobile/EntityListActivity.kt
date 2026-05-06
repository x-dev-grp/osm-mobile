package com.xdev.osm_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xdev.osm_mobile.databinding.ActivityEntityListBinding
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.models.ApiResponse
import com.xdev.osm_mobile.models.EntityListType
import com.xdev.osm_mobile.myAdapter.EntityAdapter
import com.xdev.osm_mobile.myAdapter.EntityItem
import com.xdev.osm_mobile.database.entities.LotEntity
import com.xdev.osm_mobile.database.entities.OfEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response

class EntityListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntityListBinding
    private lateinit var adapter: EntityAdapter
    private lateinit var entityType: EntityListType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEntityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val typeCode = intent.getStringExtra(EXTRA_ENTITY_TYPE)
        entityType = EntityListType.Companion.fromCode(typeCode) ?: run {
            Toast.makeText(this, "Type d'entite invalide", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        initRecyclerView()
        loadEntities()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.toolbar.title = entityType.toolbarTitle
        binding.tvListTitle.text = "Chargement..."
    }

    private fun initRecyclerView() {
        adapter = EntityAdapter(onItemClick = ::handleItemClick)
        binding.rvEntities.apply {
            layoutManager = LinearLayoutManager(this@EntityListActivity)
            adapter = this@EntityListActivity.adapter
        }
    }
    private fun loadEntities() {
        val repository = OSMApplication.repository
        lifecycleScope.launch {
            when (entityType) {
                EntityListType.LOT -> {
                    repository.allLots.collect { entities ->
                        val items = entities.map { EntityItem.Lot(it.toDto()) }
                        updateUi(items)
                    }
                }
                EntityListType.OF -> {
                    repository.allOfs.collect { entities ->
                        val items = entities.map { EntityItem.Of(it.toDto()) }
                        updateUi(items)
                    }
                }
                else -> {}
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            when (entityType) {
                EntityListType.LOT -> repository.refreshLots()
                EntityListType.OF -> repository.refreshOfs()
                else -> {}
            }
        }
    }
    private fun updateUi(items: List<EntityItem>) {
        adapter.updateData(items)
        binding.tvListTitle.text = when (items.size) {
            0 -> "Aucun element trouve"
            1 -> "1 element trouve"
            else -> "${items.size} elements trouves"
        }
    }
    private fun LotEntity.toDto() = com.xdev.osm_mobile.models.LotDto(
        id = id,
        lotNumber = lotNumber,
        deliveryNumber = deliveryNumber,
        oilQuantity = oilQuantity,
        oliveQuantity = null,
        deliveryDate = deliveryDate,
        status = status,
        supplier = com.xdev.osm_mobile.models.SupplierMinimalDto(supplierName)
    )

    private fun OfEntity.toDto() = com.xdev.osm_mobile.models.OrderFabricationDTO(
        id = id,
        code = code,
        statut = statut,
        dateDebutPrevue = dateDebutPrevue,
        dateFinPrevue = null,
        dateDebutReelle = null,
        dateFinReelle = null,
        quantiteCible = quantiteCible,
        quantiteBonne = quantiteBonne,
        quantiteNC = null,
        quantiteDefectueuse = null,
        dureeReelle = null,
        skuId = null,
        skuCode = skuCode,
        ligneId = null,
        ligneNom = ligneNom,
        lotVracId = null,
        bomId = null,
        sku = null,
        lignes = null
    )

    private suspend fun <T> fetchWrappedEntityList(
        apiCall: suspend () -> Response<ApiResponse<T>>,
        mapper: (T) -> EntityItem
    ): List<EntityItem> {
        return try {
            val response = apiCall()
            if (!response.isSuccessful) {
                Log.e(TAG, "Wrapped list fetch failed: ${response.code()}")
                return emptyList()
            }

            val items = response.body()?.data.orEmpty().map(mapper)
            Log.d(TAG, "Loaded ${items.size} ${entityType.code} items")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Wrapped list fetch crashed", e)
            emptyList()
        }
    }

    private suspend fun <T> fetchRawEntityList(
        apiCall: suspend () -> Response<List<T>>,
        mapper: (T) -> EntityItem
    ): List<EntityItem> {
        return try {
            val response = apiCall()
            if (!response.isSuccessful) {
                Log.e(TAG, "Raw list fetch failed: ${response.code()}")
                return emptyList()
            }

            val items = response.body().orEmpty().map(mapper)
            Log.d(TAG, "Loaded ${items.size} ${entityType.code} items")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Raw list fetch crashed", e)
            emptyList()
        }
    }

    private fun handleItemClick(item: EntityItem) {
        when (item) {
            is EntityItem.Lot -> {
                Toast.makeText(this, "Lot: ${item.data.lotNumber ?: "Inconnu"}", Toast.LENGTH_SHORT)
                    .show()
            }

            is EntityItem.Colis -> {
                Toast.makeText(this, "Colis: ${item.data.name ?: "Inconnu"}", Toast.LENGTH_SHORT)
                    .show()
            }

            is EntityItem.Palette -> {
                Toast.makeText(this, "Palette: ${item.data.code ?: "Inconnue"}", Toast.LENGTH_SHORT)
                    .show()
            }

            is EntityItem.Of -> {
                val ofId = item.data.id // id (UUID)
                if (!ofId.isNullOrEmpty()) {
                    startActivity(OfDetailActivity.Companion.newIntent(this, ofId))
                } else {
                    Toast.makeText(this, "ID OF invalide", Toast.LENGTH_SHORT).show()
                }
            }

            is EntityItem.OilTransaction -> {
                Toast.makeText(this, "Transaction huile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val EXTRA_ENTITY_TYPE = "extra_entity_type"
        private const val TAG = "EntityListActivity"

        fun newIntent(context: Context, type: EntityListType): Intent =
            Intent(context, EntityListActivity::class.java).apply {
                putExtra(EXTRA_ENTITY_TYPE, type.code)
            }
    }
}
