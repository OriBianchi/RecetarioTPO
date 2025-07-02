package com.example.desarrollotpo.presentation.misrecetas

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import android.text.Editable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.presentation.common.SinInternetActivity
import com.example.desarrollotpo.presentation.home.Receta
import com.example.desarrollotpo.presentation.home.RecetaAdapter
import com.example.desarrollotpo.utils.TokenUtils
import com.example.desarrollotpo.utils.setupBottomNavigation
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.text.TextWatcher
import android.widget.ImageButton
import android.widget.LinearLayout

class MisRecetasActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecetaAdapter
    private val recetas = mutableListOf<Receta>()
    private val ingredientesDisponibles = mutableSetOf<String>()
    private var search = ""
    private lateinit var loader: android.view.View
    private var sortBy = "uploadDate"
    private var sortOrder = "desc"
    private var type = "all"
    private val tiposDisponibles = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (!hayConexion()) {
            val intent = Intent(this, SinInternetActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_mis_recetas)
        loader = findViewById(R.id.progressLoader)
        setupBottomNavigation(R.id.nav_mis_recetas)

        recyclerView = findViewById(R.id.recetasRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecetaAdapter(this, recetas)
        recyclerView.adapter = adapter

        val chipOrder = findViewById<Chip>(R.id.chipOrder)
        val chipType = findViewById<Chip>(R.id.chipType)

        val btnFiltros = findViewById<ImageButton>(R.id.btnFiltros)
        val filtersContainer = findViewById<LinearLayout>(R.id.filtersContainer)

        btnFiltros.setOnClickListener {
            if (filtersContainer.visibility == android.view.View.GONE) {
                filtersContainer.visibility = android.view.View.VISIBLE
            } else {
                filtersContainer.visibility = android.view.View.GONE
            }
        }


// CHIP ORDER
        chipOrder.setOnClickListener {
            val popup = PopupMenu(this, chipOrder)
            popup.menu.add("Más reciente")
            popup.menu.add("Más antiguo")
            popup.menu.add("Alfabéticamente")

            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Más reciente" -> {
                        sortBy = "uploadDate"
                        sortOrder = "desc"
                        chipOrder.text = "Ordenado por: más reciente"
                    }
                    "Más antiguo" -> {
                        sortBy = "uploadDate"
                        sortOrder = "asc"
                        chipOrder.text = "Ordenado por: más antiguo"
                    }
                    "Alfabéticamente" -> {
                        sortBy = "name"
                        sortOrder = "asc"
                        chipOrder.text = "Ordenado por: A-Z"
                    }
                }
                fetchMisRecetas()
                true
            }
            popup.show()
        }

// CHIP TYPE
        val tiposSeleccionados = mutableSetOf<String>()
        chipType.setOnClickListener {
            if (tiposDisponibles.isEmpty()) {
                Toast.makeText(this, "Todavía no se cargaron los tipos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tiposArray = tiposDisponibles.sorted().toTypedArray()
            val seleccionadosTemp = BooleanArray(tiposArray.size) { i ->
                tiposSeleccionados.contains(tiposArray[i])
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Seleccionar tipos de receta")
            builder.setMultiChoiceItems(tiposArray, seleccionadosTemp) { _, which, isChecked ->
                if (isChecked) {
                    tiposSeleccionados.add(tiposArray[which])
                } else {
                    tiposSeleccionados.remove(tiposArray[which])
                }
            }

            builder.setPositiveButton("Aplicar") { _, _ ->
                type = if (tiposSeleccionados.isEmpty()) "all" else tiposSeleccionados.joinToString(",")
                chipType.text = if (tiposSeleccionados.isEmpty()) {
                    "Tipo: todo"
                } else {
                    "Tipo: ${tiposSeleccionados.joinToString(", ")}"
                }
                fetchMisRecetas()
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }


        val searchInput = findViewById<TextInputEditText>(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val nuevoTexto = s?.toString()?.trim() ?: ""
                if (nuevoTexto != search) {
                    search = nuevoTexto
                    fetchMisRecetas()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fetchMisRecetas()
    }

    private fun fetchMisRecetas() {
        loader.visibility = android.view.View.VISIBLE
        val token = TokenUtils.obtenerToken(this)
        if (token.isEmpty()) {
            loader.visibility = android.view.View.GONE
            Toast.makeText(this, "Debés iniciar sesión para ver tus recetas", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()

        val userRequest = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/auth/me")
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(userRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loader.visibility = android.view.View.GONE
                    Toast.makeText(this@MisRecetasActivity, "Error obteniendo usuario", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val userJsonText = response.body?.string() ?: return

                if (!response.isSuccessful || !userJsonText.trim().startsWith("{")) {
                    Log.e("MIS_RECETAS", "Error obteniendo usuario: $userJsonText")
                    return
                }

                val userJson = JSONObject(userJsonText)
                val usernameLogueado = userJson.optString("username", "")
                if (usernameLogueado.isEmpty()) return

                // ✅ A PARTIR DE ACÁ: usar los filtros correctos
                val recipesUrl = "https://desarrolloitpoapi.onrender.com/api/recipes"
                    .toHttpUrlOrNull()!!.newBuilder()
                    .addQueryParameter("name", search)

                if (type != "all") {
                    recipesUrl.addQueryParameter("classification", type)
                }

                recipesUrl.addQueryParameter("sortBy", sortBy)
                recipesUrl.addQueryParameter("sortOrder", sortOrder)

                val recipesRequest = Request.Builder()
                    .url(recipesUrl.build())
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(recipesRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            loader.visibility = android.view.View.GONE
                            Toast.makeText(this@MisRecetasActivity, "Error al cargar recetas", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseText = response.body?.string() ?: return

                        if (!response.isSuccessful || !responseText.trim().startsWith("{")) {
                            Log.e("MIS_RECETAS", "Respuesta inválida: $responseText")
                            return
                        }

                        val json = JSONObject(responseText)
                        if (!json.has("recipes")) return

                        val recetasArray = json.getJSONArray("recipes")
                        recetas.clear()

                        for (i in 0 until recetasArray.length()) {
                            val item = recetasArray.getJSONObject(i)

                            val recetaUsername = item.optString("username", "")
                            if (recetaUsername != usernameLogueado) continue

                            val name = item.getString("name")
                            val classification = item.getString("classification")
                            tiposDisponibles.add(classification)

                            val description = item.optString("description", "")
                            val steps = item.optJSONArray("steps") ?: JSONArray()
                            val ingredients = mutableListOf<String>()
                            val ingredientsList = item.optJSONArray("ingredients")

                            if (ingredientsList != null) {
                                for (j in 0 until ingredientsList.length()) {
                                    val ing = ingredientsList.getJSONObject(j)
                                    val nombre = ing.getString("name")
                                    ingredients.add(nombre)
                                    ingredientesDisponibles.add(nombre)
                                }
                            }

                            val id = item.getString("_id")
                            val image = item.optJSONArray("frontpagePhotos")?.optString(0) ?: ""
                            val uploadDate = item.optString("uploadDate", "")
                            val rating = item.optDouble("rating", 0.0)
                            val username = item.optString("username", "Desconocido")


                            val receta = Receta(
                                id = id,
                                name = name,
                                classification = classification,
                                ingredients = ingredients,
                                description = description,
                                frontImage = image,
                                author = username,
                                stepsCount = steps.length(),
                                isSaved = item.optBoolean("isSaved", false),
                                status = item.optBoolean("status", false),
                                uploadDate = uploadDate,
                                rating = rating
                            )

                            recetas.add(receta)
                        }

                        runOnUiThread {
                            loader.visibility = android.view.View.GONE
                            adapter.notifyDataSetChanged()
                        }
                    }
                })
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
