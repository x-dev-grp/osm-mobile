package com.xdev.osm_mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.xdev.osm_mobile.databinding.ActivityQcBinding
import com.xdev.osm_mobile.myAdapter.ControlPointAdapter
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.models.QCControlPointDTO
import com.xdev.osm_mobile.models.QCResultDTO
import com.xdev.osm_mobile.models.SyncRequest
import com.xdev.osm_mobile.horsligne.NetworkUtils
import com.xdev.osm_mobile.horsligne.OfflineManager
import com.xdev.osm_mobile.models.OfflineOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class QCActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQcBinding
    private lateinit var ofId: String
    private lateinit var ofCode: String
    private var points: List<QCControlPointDTO> = emptyList()
    private val values = mutableMapOf<Int, String>()
    private val photos = mutableMapOf<Int, String?>()
    private val comments = mutableMapOf<Int, String>()
    private val gson = Gson()
    private var pendingPhotoPosition: Int? = null
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null && pendingPhotoPosition != null) {
            val pos = pendingPhotoPosition!!
            photos[pos] = bitmapToBase64(bitmap)
            pendingPhotoPosition = null
            binding.rvControlPoints.adapter?.notifyItemChanged(pos)
        }
    }
    companion object {
        private const val EXTRA_OF_ID = "extra_of_id"
        private const val EXTRA_OF_CODE = "extra_of_code"
        private const val REQUEST_CAMERA_PERMISSION = 100

        fun newIntent(context: Context, ofId: String, ofCode: String): Intent =
            Intent(context, QCActivity::class.java).apply {
                putExtra(EXTRA_OF_ID, ofId)
                putExtra(EXTRA_OF_CODE, ofCode)
            }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityQcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ofId = intent.getStringExtra(EXTRA_OF_ID) ?: run {
            Toast.makeText(this, "ID OF manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        ofCode = intent.getStringExtra(EXTRA_OF_CODE) ?: "-"

        setupToolbar()
        loadControlPoints()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        supportActionBar?.title = "Contrôle qualité - $ofCode"
    }
    private fun loadControlPoints() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getActiveControlPoints(ofId)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    points = response.body()?.data ?: emptyList()

                    Log.d("QCActivity", "Points reçus : ${points.size}")

                    if (points.isEmpty()) {
                        Toast.makeText(this@QCActivity, "Aucun point de contrôle actif", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        setupRecyclerView()
                    }
                } else {
                    Toast.makeText(this@QCActivity,
                        "Erreur chargement points de contrôle",
                        Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("QCActivity", "Erreur réseau", e)
                Toast.makeText(this@QCActivity,
                    "Erreur réseau : ${e.message}",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun setupRecyclerView() {
        binding.tvOfInfo.text = "OF: $ofCode"
        val adapter = ControlPointAdapter(
            points = points,
            values = values,
            photos = photos,
            comments = comments,
            onTakePhoto = { position ->
                pendingPhotoPosition = position
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    takePhotoLauncher.launch(null)
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION
                    )
                }
            }
        )
        binding.rvControlPoints.layoutManager = LinearLayoutManager(this)
        binding.rvControlPoints.adapter = adapter
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPhotoPosition?.let { pos ->
                    pendingPhotoPosition = pos
                    takePhotoLauncher.launch(null)
                }
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupButtons() {
        binding.btnSubmit.setOnClickListener {
            submitControl()
        }
    }
    private fun submitControl() {
        if (values.size != points.size) {
            Toast.makeText(this, "Veuillez remplir tous les points de contrôle", Toast.LENGTH_SHORT).show()
            return
        }
        val signature = binding.etSignature.text.toString()
        val results = points.mapIndexed { index, point ->
            val valeur = values[index] ?: ""
            val statut = when (point.type?.uppercase()) {
                "BOOLEAN" -> {
                    when (valeur.lowercase()) {
                        "oui", "true", "ok" -> "OK"
                        "non", "false", "nok" -> "NOK"
                        else -> null
                    }
                }
                "NUMERIC" -> {
                    val numericValue = valeur.toDoubleOrNull()
                    if (numericValue != null) {
                        val min = point.minValue ?: Double.NEGATIVE_INFINITY
                        val max = point.maxValue ?: Double.POSITIVE_INFINITY
                        if (numericValue in min..max) "OK" else "NOK"
                    } else null
                }
                else -> null
            }

            QCResultDTO(
                id = null,
                controlPointId = point.id,
                ofId = ofId,
                valeur = valeur,
                statut = statut,
                commentaire = comments[index].orEmpty().ifBlank { null },
                photo = photos[index],
                signature = signature,
                dateControle = null
            )
        }
        for (result in results) {
            sendSingleResult(result)
        }
    }
    private fun sendSingleResult(result: QCResultDTO) {
        val operationId = UUID.randomUUID().toString()
        val syncRequest = SyncRequest(
            operationId = operationId,
            url = "/api/ordreConditionement/qualite/resultats/add",
            method = "POST",
            body = gson.toJson(result)
        )

        if (NetworkUtils.isInternetAvailable(this)) {
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.syncOperation(syncRequest)
                    }
                    if (response.isSuccessful) {
                        Toast.makeText(this@QCActivity, "Contrôle enregistré", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@QCActivity, "Erreur serveur (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@QCActivity, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val offlineOp = OfflineOperation(
                operationId = operationId,
                url = "/api/ordreConditionement/qualite/resultats/add",
                method = "POST",
                body = gson.toJson(result)
            )
            OfflineManager.getInstance(this).saveOperation(offlineOp)
            Toast.makeText(this, "Contrôle enregistré localement, sera synchronisé plus tard", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val bytes = stream.toByteArray()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
}
