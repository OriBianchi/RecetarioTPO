package com.example.desarrollotpo.presentation.home

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.presentation.common.SinInternetActivity
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class InicioActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecetaAdapter
    private val recetas = mutableListOf<Receta>()

    private var order = "newest"
    private var type = "all"
    private var include = ""
    private var exclude = ""
    private var author = ""
    private var saved = "all"
    private var search = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hayConexion()) {
            val intent = Intent(this, SinInternetActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_inicio)

        recyclerView = findViewById(R.id.recetasRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecetaAdapter(recetas)
        recyclerView.adapter = adapter

        val searchInput = findViewById<TextInputEditText>(R.id.searchInput)
        searchInput.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search = searchInput.text.toString().trim()
                fetchRecetas()
                return@OnEditorActionListener true
            }
            false
        })

        fetchRecetas()
    }

    private fun fetchRecetas() {
        val client = OkHttpClient()

        val urlBuilder = "https://desarrolloitpoapi.onrender.com/api/recipes"
            .toHttpUrlOrNull()!!.newBuilder()

        urlBuilder.addQueryParameter("order", order)
        urlBuilder.addQueryParameter("type", type)
        urlBuilder.addQueryParameter("include", include)
        urlBuilder.addQueryParameter("exclude", exclude)
        urlBuilder.addQueryParameter("author", author)
        urlBuilder.addQueryParameter("saved", saved)
        urlBuilder.addQueryParameter("search", search)

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

                val jsonBody = response.body?.string() ?: "{}"
                val jsonObject = JSONObject(jsonBody)
                val jsonArray = jsonObject.getJSONArray("recipes")

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
                    val image = item.optJSONArray("frontpagePhotos")?.optString(0) ?: ""

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

    private fun hayConexion(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
