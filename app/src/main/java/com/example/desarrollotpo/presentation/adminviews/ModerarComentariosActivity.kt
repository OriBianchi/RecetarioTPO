package com.example.desarrollotpo.presentation.adminviews

import ComentarioPendiente
import ComentarioPendienteAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.utils.TokenUtils
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ModerarComentariosActivity : BaseActivity() {

    private lateinit var loaderOverlay: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ComentarioPendienteAdapter
    private val comentarios = mutableListOf<ComentarioPendiente>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_moderar_comentarios)

        loaderOverlay = findViewById(R.id.loaderOverlay)
        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ModerarComentariosActivity)
        }

        val contenedor = findViewById<LinearLayout>(R.id.contenedorComentarios)
        contenedor.removeAllViews()
        contenedor.addView(recyclerView)

        adapter = ComentarioPendienteAdapter(
            context = this,
            comentarios = comentarios,
            onAprobar = { aprobarComentario(it) },
            onRechazar = { rechazarComentario(it) }
        )
        recyclerView.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        cargarComentarios()
    }

    private fun cargarComentarios() {
        mostrarLoader()

        val token = TokenUtils.obtenerToken(this)
        if (token.isEmpty()) {
            Toast.makeText(this, "Token no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/recipes/comments/pending")
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    ocultarLoader()
                    Toast.makeText(this@ModerarComentariosActivity, "Error de red", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { ocultarLoader() }

                if (!response.isSuccessful) {
                    Log.e("MODERAR", "Error HTTP: ${response.code}")
                    return
                }

                val jsonArray = JSONArray(response.body?.string() ?: return)
                comentarios.clear()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val user = obj.getJSONObject("user")
                    comentarios.add(
                        ComentarioPendiente(
                            recipeId = obj.getString("recipeId"),
                            recipeName = obj.getString("recipeName"),
                            commentId = obj.getString("commentId"),
                            text = obj.getString("text"),
                            username = user.getString("username"),
                            createdAt = obj.getString("createdAt")
                        )
                    )
                }

                runOnUiThread {
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }

    private fun aprobarComentario(item: ComentarioPendiente) {
        val token = TokenUtils.obtenerToken(this)
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/${item.recipeId}/comments/${item.commentId}/approve"

        val request = Request.Builder()
            .url(url)
            .patch(RequestBody.create(null, ByteArray(0)))
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ModerarComentariosActivity, "Error al aprobar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ModerarComentariosActivity, "Aprobado", Toast.LENGTH_SHORT).show()
                        comentarios.remove(item)
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@ModerarComentariosActivity, "Error al aprobar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun rechazarComentario(item: ComentarioPendiente) {
        val token = TokenUtils.obtenerToken(this)
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/${item.recipeId}/comments/${item.commentId}/reject"

        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ModerarComentariosActivity, "Error al rechazar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ModerarComentariosActivity, "Rechazado", Toast.LENGTH_SHORT).show()
                        comentarios.remove(item)
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@ModerarComentariosActivity, "Error al rechazar", Toast.LENGTH_SHORT).show()
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
