package com.example.desarrollotpo.presentation.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.desarrollotpo.R
import com.google.android.material.button.MaterialButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.google.android.gms.auth.api.credentials.*
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.common.api.ResolvableApiException
import android.widget.TextView
import android.widget.ImageView
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.presentation.common.WelcomeActivity
import com.example.desarrollotpo.presentation.forgotPassword.ForgotPasswordActivity
import com.example.desarrollotpo.presentation.home.InicioActivity
import org.json.JSONObject

class LoginFormActivity : BaseActivity() {

    private lateinit var credentialsClient: CredentialsClient
    private val CREDENTIAL_SAVE_REQUEST = 1001

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_form)

        credentialsClient = Credentials.getClient(this)

        val emailEditText = findViewById<EditText>(R.id.emailInput)
        val passwordEditText = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<MaterialButton>(R.id.confirmLoginButton)
        val backInicio = findViewById<ImageView>(R.id.backInicio)
        val forgotPasswordText = findViewById<TextView>(R.id.olvidocontra)



        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }


        backInicio.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            when {
                email.isEmpty() || password.isEmpty() ->
                    Toast.makeText(this, "Completá todos los campos", Toast.LENGTH_SHORT).show()
                !isValidEmail(email) ->
                    Toast.makeText(this, "Email inválido", Toast.LENGTH_SHORT).show()
                password.length < 6 ->
                    Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                else -> {
                    val client = OkHttpClient()
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val requestBody = """{"email":"$email", "password":"$password"}""".toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("https://desarrolloitpoapi.onrender.com/api/auth/login")
                        .post(requestBody)
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                Toast.makeText(this@LoginFormActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            runOnUiThread {
                                if (response.isSuccessful) {
                                    val json = response.body?.string()
                                    val token = JSONObject(json).optString("token")

                                    if (token.isNotEmpty()) {
                                        com.example.desarrollotpo.utils.TokenUtils.guardarToken(this@LoginFormActivity, token)
                                        Toast.makeText(this@LoginFormActivity, "¡Login exitoso! 🎉", Toast.LENGTH_SHORT).show()
                                        saveCredential(email, password)
                                        startActivity(Intent(this@LoginFormActivity, InicioActivity::class.java))
                                    } else {
                                        Toast.makeText(this@LoginFormActivity, "Token no recibido 😕", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            }
                        }
                    })
                }
            }
        }
    }

    private fun saveCredential(email: String, password: String) {
        val credential = Credential.Builder(email)
            .setPassword(password)
            .build()

        credentialsClient.save(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Credencial guardada 🔐", Toast.LENGTH_SHORT).show()
            } else {
                val e = task.exception
                if (e is ResolvableApiException) {
                    try {
                        e.startResolutionForResult(this, CREDENTIAL_SAVE_REQUEST)
                    } catch (ex: Exception) {
                        Toast.makeText(this, "No se pudo mostrar el diálogo", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No se pudo guardar la credencial 😓", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREDENTIAL_SAVE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Credencial guardada ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Guardado cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
