package com.example.desarrollotpo.presentation.adminviews

import RecetaPendienteAdapter
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
import com.example.desarrollotpo.presentation.home.Receta
import com.example.desarrollotpo.utils.TokenUtils
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ModerarRecetasActivity : BaseActivity() {

    private lateinit var loaderOverlay: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecetaPendienteAdapter
    private val recetasPendientes = mutableListOf<Receta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_moderar_recetas)

        loaderOverlay = findViewById(R.id.loaderOverlay)
        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ModerarRecetasActivity)
        }

        val contenedor = findViewById<LinearLayout>(R.id.contenedorRecetas)
        contenedor.removeAllViews()
        contenedor.addView(recyclerView)

        adapter = RecetaPendienteAdapter(
            context = this,
            recetas = recetasPendientes,
            onAprobada = { aprobarReceta(it) },
            onRechazada = { rechazarReceta(it) }
        )
        recyclerView.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        cargarRecetasPendientes()
    }

    private fun cargarRecetasPendientes() {
        mostrarLoader()

        val token = TokenUtils.obtenerToken(this)
        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/recipes/pending")
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    ocultarLoader()
                    Toast.makeText(this@ModerarRecetasActivity, "Error de red", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { ocultarLoader() }

                if (!response.isSuccessful) {
                    Log.e("MODERAR_RECETAS", "Error HTTP: ${response.code}")
                    return
                }

                val body = JSONObject(response.body?.string() ?: return)
                val array = body.getJSONArray("pendingRecipes")

                recetasPendientes.clear()

                for (i in 0 until array.length()) {
                    val r = array.getJSONObject(i)
                    val ingredientes = r.getJSONArray("ingredients")
                    val listaIngredientes = mutableListOf<String>()
                    for (j in 0 until ingredientes.length()) {
                        listaIngredientes.add(ingredientes.getJSONObject(j).getString("name"))
                    }

                    val receta = Receta(
                        id = r.getString("_id"),
                        name = r.getString("name"),
                        classification = r.getString("classification"),
                        ingredients = listaIngredientes,
                        description = r.optString("description", ""),
                        frontImage = r.optJSONArray("frontpagePhotos")?.optString(0) ?: "",
                        author = r.optString("username", "Desconocido"),
                        stepsCount = r.optInt("stepCount", 0),
                        isSaved = false,
                        status = false,
                        uploadDate = r.optString("uploadDate", ""),
                        rating = r.optDouble("rating", 0.0),
                        portions = r.optInt("portions", 1),
                        stepsJson = "[]",
                        ingredientsJson = ingredientes.toString()
                    )

                    recetasPendientes.add(receta)
                }

                runOnUiThread {
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }

    private fun aprobarReceta(receta: Receta) {
        mostrarLoader()

        val token = TokenUtils.obtenerToken(this)
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/${receta.id}/approve"

        val request = Request.Builder()
            .url(url)
            .patch(RequestBody.create(null, ByteArray(0)))
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    ocultarLoader()
                    Toast.makeText(this@ModerarRecetasActivity, "Error al aprobar receta", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    ocultarLoader()
                    if (response.isSuccessful) {
                        Toast.makeText(this@ModerarRecetasActivity, "Receta aprobada", Toast.LENGTH_SHORT).show()
                        recetasPendientes.remove(receta)
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@ModerarRecetasActivity, "Error al aprobar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun rechazarReceta(receta: Receta) {
        mostrarLoader()

        val token = TokenUtils.obtenerToken(this)
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/${receta.id}/reject"

        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    ocultarLoader()
                    Toast.makeText(this@ModerarRecetasActivity, "Error al rechazar receta", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    ocultarLoader()
                    if (response.isSuccessful) {
                        Toast.makeText(this@ModerarRecetasActivity, "Receta rechazada", Toast.LENGTH_SHORT).show()
                        recetasPendientes.remove(receta)
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@ModerarRecetasActivity, "Error al rechazar", Toast.LENGTH_SHORT).show()
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
    override fun onResume() {
        super.onResume()
        cargarRecetasPendientes()
    }

}
