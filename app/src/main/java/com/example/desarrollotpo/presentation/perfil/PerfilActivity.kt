package com.example.desarrollotpo.presentation.perfil

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.desarrollotpo.R
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.utils.TokenUtils
import com.example.desarrollotpo.utils.setupBottomNavigation
import com.google.android.material.button.MaterialButton
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.desarrollotpo.presentation.adminviews.ModerarComentariosActivity
import com.example.desarrollotpo.presentation.adminviews.ModerarRecetasActivity

class PerfilActivity : BaseActivity() {

    private val client = OkHttpClient()
    private lateinit var loaderOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)
        setupBottomNavigation(R.id.nav_perfil)

        loaderOverlay = findViewById(R.id.loaderOverlay)

        obtenerDatosUsuario()

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            TokenUtils.borrarToken(this)
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun obtenerDatosUsuario() {
        val token = TokenUtils.obtenerToken(this)

        if (token.isEmpty()) {
            Toast.makeText(this, "No has iniciado sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mostrarLoader()

        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/auth/me")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Perfil", "Error de red: ${e.message}")
                runOnUiThread {
                    ocultarLoader()
                    Toast.makeText(this@PerfilActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    ocultarLoader()
                }

                if (!response.isSuccessful) {
                    Log.e("Perfil", "Respuesta no exitosa: ${response.code}")
                    return
                }

                val json = JSONObject(response.body?.string() ?: return)
                val username = json.getString("username")
                val role = json.getString("role")
                val savedRecipes = json.getJSONArray("savedRecipes")
                val cantidad = savedRecipes.length()

                runOnUiThread {
                    findViewById<TextView>(R.id.tvUsername).text = username
                    findViewById<TextView>(R.id.tvUserType).text = "Tipo: ${if (role == "admin") "Admin" else "Usuario"}"

                    val adminButtons = findViewById<View>(R.id.adminButtonsContainer)

                    if (role == "admin") {
                        adminButtons.visibility = View.VISIBLE

                        findViewById<MaterialButton>(R.id.btnModerarRecetas).setOnClickListener {
                            startActivity(Intent(this@PerfilActivity, ModerarRecetasActivity::class.java))
                        }

                        findViewById<MaterialButton>(R.id.btnModerarComentarios).setOnClickListener {
                            startActivity(Intent(this@PerfilActivity, ModerarComentariosActivity::class.java))
                        }
                    }
                }
            }
        })
    }

    private fun mostrarLoader() {
        loaderOverlay.visibility = View.VISIBLE
    }

    private fun ocultarLoader() {
        loaderOverlay.visibility = View.GONE
    }
}
