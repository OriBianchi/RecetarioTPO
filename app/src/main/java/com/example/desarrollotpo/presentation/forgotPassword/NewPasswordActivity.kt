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

class NewPasswordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hayConexion()) {
            val intent = Intent(this, SinInternetActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_new_password)

        val backInicio = findViewById<ImageView>(R.id.backInicio)
        val nuevaPasswordInput = findViewById<TextInputEditText>(R.id.NewPasswordInput)
        val repetirPasswordInput = findViewById<TextInputEditText>(R.id.reescribirPasswordInput)
        val cambiarBtn = findViewById<MaterialButton>(R.id.CambiarContraseñaButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val resetCode = intent.getStringExtra("resetCode") ?: ""

        backInicio.setOnClickListener {
            startActivity(Intent(this, CodgoCorreoActivity::class.java))
        }

        cambiarBtn.setOnClickListener {
            val password = nuevaPasswordInput.text.toString().trim()
            val repetir = repetirPasswordInput.text.toString().trim()

            if (password.isEmpty() || repetir.isEmpty()) {
                Toast.makeText(this, "Completá ambos campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val permitido = "^[a-zA-Z0-9@#\$%^&+=!?.]*$".toRegex()
            if (!permitido.matches(password) || !permitido.matches(repetir)) {
                Toast.makeText(this, "La contraseña solo puede tener letras, números y símbolos comunes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            if (password != repetir) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            cambiarBtn.isEnabled = false

            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = """
                {
                    "resetCode": "$resetCode",
                    "newPassword": "$password"
                }
            """.trimIndent().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://desarrolloitpoapi.onrender.com/api/auth/reset-password")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        cambiarBtn.isEnabled = true
                        Toast.makeText(this@NewPasswordActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        cambiarBtn.isEnabled = true
                        when (response.code) {
                            200 -> {
                                Toast.makeText(this@NewPasswordActivity, "¡Contraseña actualizada!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@NewPasswordActivity, LoginFormActivity::class.java))
                            }
                            400 -> Toast.makeText(this@NewPasswordActivity, "Código inválido o expirado", Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(this@NewPasswordActivity, "Error del servidor", Toast.LENGTH_SHORT).show()
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
