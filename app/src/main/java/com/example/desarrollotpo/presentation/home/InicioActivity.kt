package com.example.desarrollotpo.data.model.desarrollotpo.presentation.home

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import java.io.IOException

class InicioActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecetaAdapter
    private val recetas = mutableListOf<Receta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        recyclerView = findViewById(R.id.recetasRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecetaAdapter(recetas)
        recyclerView.adapter = adapter

        fetchRecetas()
    }

    private fun fetchRecetas() {
        val client = OkHttpClient()

        val urlBuilder = "https://desarrolloitpoapi.onrender.com/api/recipes"
            .toHttpUrlOrNull()!!.newBuilder()

        // Filtros por defecto (simulan lo que se ve en la UI)
        urlBuilder.addQueryParameter("order", "newest")
        urlBuilder.addQueryParameter("type", "all")
        urlBuilder.addQueryParameter("exclude", "")
        urlBuilder.addQueryParameter("include", "")
        urlBuilder.addQueryParameter("author", "")
        urlBuilder.addQueryParameter("saved", "all")

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@InicioActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@InicioActivity, "Error al cargar recetas", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val jsonBody = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(jsonBody)

                recetas.clear()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)

                    val name = item.getString("name")
                    val classification = item.getString("classification")
                    val description = item.optString("description", "")
                    val ingredientsList = item.optJSONArray("ingredients")
                    val ingredients = mutableListOf<String>()
                    if (ingredientsList != null) {
                        for (j in 0 until ingredientsList.length()) {
                            val ing = ingredientsList.getJSONObject(j)
                            ingredients.add(ing.getString("name"))
                        }
                    }

                    val user = item.optJSONObject("userId")
                    val username = user?.optString("username") ?: "Desconocido"
                    val steps = item.optJSONArray("steps") ?: JSONArray()
                    val image = item.optJSONArray("frontpagePhotos")?.optString(0)
                        ?: "https://via.placeholder.com/100"

                    val receta = Receta(
                        name = name,
                        classification = classification,
                        ingredients = ingredients,
                        description = description,
                        frontImage = image,
                        author = username,
                        stepsCount = steps.length()
                    )
                    recetas.add(receta)
                }

                runOnUiThread {
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }
}
