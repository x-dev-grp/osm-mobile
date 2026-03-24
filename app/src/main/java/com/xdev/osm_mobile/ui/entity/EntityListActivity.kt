package com.xdev.osm_mobile.ui.entity

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
import com.xdev.osm_mobile.network.models.ApiResponse
import com.xdev.osm_mobile.ui.entity.adapters.EntityAdapter
import com.xdev.osm_mobile.ui.entity.adapters.EntityItem
import com.xdev.osm_mobile.ui.of.OfDetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        entityType = EntityListType.fromCode(typeCode) ?: run {
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
        lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    fetchEntitiesForType()
                }

                adapter.updateData(items)
                binding.tvListTitle.text = when (items.size) {
                    0 -> "Aucun element trouve"
                    1 -> "1 element trouve"
                    else -> "${items.size} elements trouves"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to load ${entityType.code} list", e)
                adapter.updateData(emptyList())
                binding.tvListTitle.text = "Erreur de chargement"
                Toast.makeText(
                    this@EntityListActivity,
                    e.message ?: "Erreur inconnue",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun fetchEntitiesForType(): List<EntityItem> =
        when (entityType) {
            EntityListType.LOT -> fetchWrappedEntityList(
                apiCall = { RetrofitClient.instance.getLots() },
                mapper = { EntityItem.Lot(it) }
            )

            EntityListType.COLIS -> fetchWrappedEntityList(
                apiCall = { RetrofitClient.instance.getColis() },
                mapper = { EntityItem.Colis(it) }
            )

            EntityListType.PALETTE -> fetchWrappedEntityList(
                apiCall = { RetrofitClient.instance.getPalettes() },
                mapper = { EntityItem.Palette(it) }
            )

            EntityListType.OF -> fetchRawEntityList(
                apiCall = { RetrofitClient.instance.getOfs() },
                mapper = { EntityItem.Of(it) }
            )
        }

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
                val ofNumber = item.data.code
                if (!ofNumber.isNullOrEmpty()) {
                    startActivity(OfDetailActivity.newIntent(this, ofNumber))
                } else {
                    Toast.makeText(this, "Numero OF invalide", Toast.LENGTH_SHORT).show()
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
