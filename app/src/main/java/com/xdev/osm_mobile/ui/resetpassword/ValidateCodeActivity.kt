package com.xdev.osm_mobile.ui.resetpassword

import android.content.Intent
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
 * Step 2 of password reset: enter the code received by email.
 */
class ValidateCodeActivity : AppCompatActivity() {

    private lateinit var etCode: EditText
    private lateinit var btnValidate: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMessage: TextView
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getStringExtra("user_id") ?: ""
        setContentView(buildLayout())
        supportActionBar?.title = "Enter Reset Code"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }

    private fun validateCode() {
        val code = etCode.text.toString().trim()
        if (code.isEmpty()) {
            etCode.error = "Enter the code"; return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.validateResetCode(userId, code)
                }
                if (response.isSuccessful) {
                    val intent = Intent(this@ValidateCodeActivity, NewPasswordActivity::class.java)
                    intent.putExtra("user_id", userId)
                    startActivity(intent)
                    finish()
                } else {
                    showMessage("Invalid or expired code (HTTP ${response.code()})")
                }
            } catch (e: Exception) {
                showMessage("Network error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnValidate.isEnabled = !loading
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
            text = "Enter the reset code sent to your email."
            textSize = 14f
            setPadding(0, 0, 0, (24 * dp).toInt())
        })
        etCode = EditText(this).apply {
            hint = "Reset Code"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (16 * dp).toInt() }
        }
        root.addView(etCode)
        btnValidate = Button(this).apply {
            text = "Validate Code"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { validateCode() }
        }
        root.addView(btnValidate)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
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
