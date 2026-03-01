package com.xdev.osm_mobile.ui.resetpassword

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.xdev.osm_mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Step 1 of password reset: enter username/email to receive a reset code.
 */
class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etIdentifier: EditText
    private lateinit var btnSend: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
        supportActionBar?.title = "Reset Password"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun sendResetCode() {
        val identifier = etIdentifier.text.toString().trim()
        if (identifier.isEmpty()) {
            etIdentifier.error = "Please enter your username or email"
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.resetPassword(identifier)
                }
                if (response.isSuccessful) {
                    val userId = response.body()?.id ?: ""
                    tvMessage.text = "Reset code sent! Check your email."
                    tvMessage.visibility = View.VISIBLE
                    // Navigate to validate code screen
                    val intent = android.content.Intent(
                        this@ResetPasswordActivity,
                        ValidateCodeActivity::class.java
                    )
                    intent.putExtra("user_id", userId)
                    startActivity(intent)
                } else {
                    tvMessage.text = "Error: ${response.code()} — user not found?"
                    tvMessage.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                tvMessage.text = "Network error: ${e.message}"
                tvMessage.visibility = View.VISIBLE
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSend.isEnabled = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun buildLayout(): View {
        val dp = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding((32 * dp).toInt(), (48 * dp).toInt(), (32 * dp).toInt(), (32 * dp).toInt())
        }
        root.addView(TextView(this).apply {
            text = "Enter your username or email to receive a reset code."
            textSize = 14f
            setPadding(0, 0, 0, (24 * dp).toInt())
        })
        etIdentifier = EditText(this).apply {
            hint = "Username or Email"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (16 * dp).toInt() }
        }
        root.addView(etIdentifier)
        btnSend = Button(this).apply {
            text = "Send Reset Code"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { sendResetCode() }
        }
        root.addView(btnSend)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * dp).toInt() }
        }
        root.addView(progressBar)
        tvMessage = TextView(this).apply {
            visibility = View.GONE
            textSize = 13f
            setTextColor(0xFF1565C0.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (16 * dp).toInt() }
        }
        root.addView(tvMessage)
        return root
    }
}
