package com.xdev.osm_mobile.ui.entity

// Import des bibliothèques nécessaires
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

/**
 * Activité générique pour afficher une liste d'entités (Lots, Colis, Palettes, OF).
 * Elle est appelée avec un type d'entité passé en Intent (via EntityListType).
 * Affiche une liste de cartes avec les informations principales de chaque entité.
 * Au clic sur une carte, l'utilisateur est redirigé vers la page de détail correspondante.
 */
class EntityListActivity : AppCompatActivity() {

    // Binding pour accéder aux vues du layout (activity_entity_list.xml)
    private lateinit var binding: ActivityEntityListBinding

    // Adaptateur personnalisé qui gère l'affichage de tous les types d'entités
    private lateinit var adapter: EntityAdapter

    // Type d'entité (Lot, Colis, Palette, OF) passé par l'Intent
    private lateinit var entityType: EntityListType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisation du binding (vue)
        binding = ActivityEntityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Récupération du type d'entité depuis l'Intent
        val EXTRA_ENTITY_TYPE = null
        val typeCode = intent.getStringExtra(EXTRA_ENTITY_TYPE)
        entityType = EntityListType.fromCode(typeCode) ?: run {
            // Si le type est invalide, on ferme l'activité après un message d'erreur
            Toast.makeText(this, "Type d'entité invalide", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configuration de la barre d'outils (Toolbar)
        setupToolbar()

        // Initialisation du RecyclerView avec l'adaptateur
        initRecyclerView()

        // Chargement des données depuis l'API (appel asynchrone)
        loadEntities()
    }

    /**
     * Configure la Toolbar : titre, flèche de retour.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)      // Flèche de retour
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        binding.toolbar.title = entityType.toolbarTitle        // Titre dynamique
        binding.tvListTitle.text = "Liste des ${entityType.displayName}"
    }

    /**
     * Initialise le RecyclerView et l'adaptateur.
     * L'adaptateur est créé avec un callback pour gérer les clics.
     */
    private fun initRecyclerView() {
        adapter = EntityAdapter { item -> handleItemClick(item) }
        binding.rvEntities.apply {
            layoutManager = LinearLayoutManager(this@EntityListActivity)
            adapter = this@EntityListActivity.adapter
        }
    }

    /**
     * Charge la liste des entités depuis l'API, de manière asynchrone.
     * Utilise les coroutines (lifecycleScope) pour ne pas bloquer l'UI.
     */
    private fun loadEntities() {
        lifecycleScope.launch(Dispatchers.IO) {   // Exécution en arrière-plan
            try {
                // Récupération des données selon le type d'entité
                val items = fetchEntitiesForType()

                // Mise à jour de l'UI sur le thread principal
                withContext(Dispatchers.Main) {
                    adapter.updateData(items)   // Met à jour le RecyclerView
                    binding.tvListTitle.text = when (items.size) {
                        0 -> "Aucun élément trouvé"
                        1 -> "1 élément trouvé"
                        else -> "${items.size} éléments trouvés"
                    }
                }
            } catch (e: Exception) {
                // En cas d'erreur, affiche un message et vide la liste
                withContext(Dispatchers.Main) {
                    adapter.updateData(emptyList())
                    binding.tvListTitle.text = "Erreur de chargement"
                    Toast.makeText(
                        this@EntityListActivity,
                        "Erreur: ${e.message ?: "Inconnue"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Appelle l'API appropriée selon le type d'entité et retourne une liste d'EntityItem.
     * Utilise une fonction générique fetchEntityList pour factoriser le code.
     */
    private suspend fun fetchEntitiesForType(): List<EntityItem> {
        return when (entityType) {
            EntityListType.LOT -> fetchEntityList(
                { RetrofitClient.instance.getLots() },
                { data -> EntityItem.Lot(data) }
            )

            EntityListType.COLIS -> fetchEntityList(
                { RetrofitClient.instance.getColis() },
                { data -> EntityItem.Colis(data) }
            )

            EntityListType.PALETTE -> fetchEntityList(
                { RetrofitClient.instance.getPalettes() },
                { data -> EntityItem.Palette(data) }
            )

            EntityListType.OF -> fetchEntityList(
                { RetrofitClient.instance.getOfs() },
                { data -> EntityItem.Of(data) }
            )

            else -> emptyList()   // Ne devrait jamais arriver
        }
    }

    /**
     * Fonction générique pour exécuter un appel API et mapper la réponse en EntityItem.
     * @param apiCall fonction qui retourne une Response<ApiResponse<T>>
     * @param mapper fonction qui transforme T en EntityItem
     * @return Liste d'EntityItem (vide si erreur)
     */
    private suspend fun <T> fetchEntityList(
        apiCall: suspend () -> Response<ApiResponse<T>>,
        mapper: (T) -> EntityItem
    ): List<EntityItem> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                // Extraire les données du wrapper ApiResponse et les mapper
                response.body()?.data?.map(mapper) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()   // En cas d'erreur réseau, liste vide
        }
    }

    /**
     * Gère le clic sur un élément de la liste.
     * Selon le type d'entité, ouvre la page de détail correspondante.
     */
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

            // Cas des Ordres de Fabrication (OF)
            is EntityItem.Of -> {
                val ofNumber = item.data.code
                if (!ofNumber.isNullOrEmpty()) {
                    //ouvrir le détail de l'OF
                    val intent = OfDetailActivity.newIntent(this, ofNumber)
                    startActivity(intent)

                }
            }

            companion object {
                // Clé pour passer le type d'entité dans l'Intent
                private const val EXTRA_ENTITY_TYPE = "extra_entity_type"

                /**
                 * Méthode utilitaire pour créer un Intent de lancement de cette activité.
                 * @param context Contexte d'appel
                 * @param type Type d'entité à afficher
                 * @return Intent prêt à être démarré
                 */
                fun newIntent(context: Context, type: EntityListType): Intent =
                    Intent(context, EntityListActivity::class.java).apply {
                        putExtra(EXTRA_ENTITY_TYPE, type.code)
                    }
            }
        }
    }
}