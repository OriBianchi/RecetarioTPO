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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class CodgoCorreoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hayConexion()) {
            val intent = Intent(this, SinInternetActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_codgo_correo)

        val backInicio = findViewById<ImageView>(R.id.backInicio)
        val verificarButton = findViewById<MaterialButton>(R.id.VerificarCodigoButton)
        val codigoInput = findViewById<TextInputEditText>(R.id.codigoInput)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        backInicio.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        verificarButton.setOnClickListener {
            val codigo = codigoInput.text.toString().trim()

            if (codigo.isEmpty()) {
                Toast.makeText(this, "Ingresá el código", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            verificarButton.isEnabled = false

            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = """{"resetCode":"$codigo"}""".toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://desarrolloitpoapi.onrender.com/api/auth/verify-reset-code")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        verificarButton.isEnabled = true
                        Toast.makeText(this@CodgoCorreoActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        verificarButton.isEnabled = true

                        if (response.isSuccessful) {
                            Toast.makeText(this@CodgoCorreoActivity, "¡Código verificado!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@CodgoCorreoActivity, NewPasswordActivity::class.java)
                            intent.putExtra("resetCode", codigo)
                            startActivity(intent)
                        } else if (response.code == 400) {
                            Toast.makeText(this@CodgoCorreoActivity, "Código inválido o expirado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CodgoCorreoActivity, "Error del servidor", Toast.LENGTH_SHORT).show()
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
