package com.xdev.osm_mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.onesignal.OneSignal
import com.xdev.osm_mobile.databinding.ActivityLoginBinding
import com.xdev.osm_mobile.login.LoginViewModel
import com.xdev.osm_mobile.models.JwtUtils
import com.xdev.osm_mobile.models.RegistrationRequest
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (OSMApplication.sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }
        setupUI()
        setupObservers()
    }
    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateInput(username, password)) {
                viewModel.login(username, password, OSMApplication.sessionManager)
            }
        }
    }
    private fun validateInput(username: String, password: String): Boolean {
        return when {
            username.isEmpty() -> {
                binding.etUsername.error = "Username required"
                false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password required"
                false
            }
            else -> true
        }
    }
    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.progressBar.isVisible = isLoading
        }
        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { authResponse ->
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                authResponse.accessToken?.let { token ->
                    authResponse.osmUser?.let { user ->
                        OSMApplication.sessionManager.saveUser(user)
                    }
                    val userId = JwtUtils.getUserId(token) ?: authResponse.osmUser?.username
                    registerDeviceForPush(userId)
                }
                navigateToMain()
            }.onFailure { exception ->
                Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun registerDeviceForPush(userId: String?) {
        if (userId.isNullOrBlank()) return
        lifecycleScope.launch {
            val playerId = OneSignal.User.pushSubscription.id
            if (!playerId.isNullOrBlank()) {
                try {
                    val response = RetrofitClient.instance.registerDevice(
                        RegistrationRequest(userId = userId, playerId = playerId)
                    )
                    if (response.isSuccessful) {
                        Log.d("DeviceReg", "✅ Device enregistré: $playerId")
                    } else {
                        Log.e("DeviceReg", " Erreur ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("DeviceReg", " Erreur réseau: ${e.message}")
                }
            } else {
                Log.w("DeviceReg", " PlayerID non disponible – sera enregistré via observer")
            }
        }
    }
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
