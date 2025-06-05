package com.example.desarrollotpo.presentation.forgotPassword

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.desarrollotpo.R
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.presentation.common.SinInternetActivity
import com.example.desarrollotpo.presentation.login.LoginFormActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ForgotPasswordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hayConexion()) {
            val intent = Intent(this, SinInternetActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_forgot_password)

        val emailEditText = findViewById<TextInputEditText>(R.id.emailInput)
        val enviarButton = findViewById<MaterialButton>(R.id.enviarCodigoButton)
        val backInicio = findViewById<ImageView>(R.id.backInicio)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        backInicio.setOnClickListener {
            startActivity(Intent(this, LoginFormActivity::class.java))
        }

        enviarButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresá tu correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "El correo no es válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            enviarButton.isEnabled = false

            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = """{"email":"$email"}""".toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://desarrolloitpoapi.onrender.com/api/auth/request-reset")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        enviarButton.isEnabled = true
                        Toast.makeText(this@ForgotPasswordActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        enviarButton.isEnabled = true

                        when (response.code) {
                            200 -> {
                                Toast.makeText(this@ForgotPasswordActivity, "¡Correo enviado!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@ForgotPasswordActivity, CodgoCorreoActivity::class.java)
                                intent.putExtra("email", email)
                                startActivity(intent)
                            }
                            400 -> Toast.makeText(this@ForgotPasswordActivity, "Correo no registrado", Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(this@ForgotPasswordActivity, "Error del servidor", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    private fun hayConexion(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
