package com.xdev.osm_mobile.ui.resetpassword

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.network.models.UpdatePasswordRequest
import com.xdev.osm_mobile.ui.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Step 3 of password reset (also used for first-login forced password change):
 * enter and confirm a new password.
 */
class NewPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnUpdate: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMessage: TextView
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getStringExtra("user_id") ?: ""
        setContentView(buildLayout())
        supportActionBar?.title = "Set New Password"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }

    private fun updatePassword() {
        val newPass = etNewPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()

        if (newPass.isEmpty()) {
            etNewPassword.error = "Enter new password"; return
        }
        if (newPass != confirm) {
            etConfirmPassword.error = "Passwords do not match"; return
        }
        if (newPass.length < 6) {
            etNewPassword.error = "Minimum 6 characters"; return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.updatePassword(
                        userId = userId,
                        request = UpdatePasswordRequest(newPass, confirm)
                    )
                }
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@NewPasswordActivity,
                        "Password updated! Please log in.", Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(this@NewPasswordActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                } else {
                    showMessage("Failed to update password (HTTP ${response.code()})")
                }
            } catch (e: Exception) {
                showMessage("Network error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnUpdate.isEnabled = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showMessage(msg: String) {
        tvMessage.text = msg
        tvMessage.visibility = View.VISIBLE
    }

    private fun buildLayout(): View {
        val dp = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding((32 * dp).toInt(), (48 * dp).toInt(), (32 * dp).toInt(), (32 * dp).toInt())
        }
        root.addView(TextView(this).apply {
            text = "Choose a new password."
            textSize = 14f
            setPadding(0, 0, 0, (24 * dp).toInt())
        })
        etNewPassword = EditText(this).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (16 * dp).toInt() }
        }
        root.addView(etNewPassword)
        etConfirmPassword = EditText(this).apply {
            hint = "Confirm Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * dp).toInt() }
        }
        root.addView(etConfirmPassword)
        btnUpdate = Button(this).apply {
            text = "Update Password"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { updatePassword() }
        }
        root.addView(btnUpdate)
        progressBar = ProgressBar(this).apply { visibility = View.GONE }
        root.addView(progressBar)
        tvMessage = TextView(this).apply {
            visibility = View.GONE
            textSize = 13f
            setTextColor(0xFFCC0000.toInt())
        }
        root.addView(tvMessage)
        return root
    }
}
