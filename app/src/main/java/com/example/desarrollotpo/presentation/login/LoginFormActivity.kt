package com.example.desarrollotpo.presentation.login

import android.content.Context
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
import android.widget.TextView
import android.widget.ImageView
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.presentation.common.WelcomeActivity
import com.example.desarrollotpo.presentation.forgotPassword.ForgotPasswordActivity
import com.example.desarrollotpo.presentation.home.InicioActivity
import org.json.JSONObject

class LoginFormActivity : BaseActivity() {

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_form)

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

        // Recuperar email si está guardado
        val prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
        val emailGuardado = prefs.getString("email", "")
        emailEditText.setText(emailGuardado)

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

                                        // Guardar email y token en SharedPreferences
                                        val prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
                                        with(prefs.edit()) {
                                            putString("email", email)
                                            putString("token", token)
                                            apply()
                                        }

                                        Toast.makeText(this@LoginFormActivity, "¡Login exitoso! 🎉", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this@LoginFormActivity, InicioActivity::class.java))
                                    } else {
                                        Toast.makeText(this@LoginFormActivity, "Token no recibido 😕", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this@LoginFormActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                }
            }
        }
    }
}
