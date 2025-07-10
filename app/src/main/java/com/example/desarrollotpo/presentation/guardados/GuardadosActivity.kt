package com.example.desarrollotpo.presentation.guardados

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu

class GuardadosActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecetaAdapter
    private lateinit var loadingSpinner: ProgressBar
    private val recetas = mutableListOf<Receta>()
    private val ingredientesDisponibles = mutableSetOf<String>()
    private var search = ""
    private var include = ""
    private var exclude = ""
    private var sortBy = "name"
    private var sortOrder = "asc"
    private var type = "all"
    private var author = ""
    private val tiposDisponibles = mutableSetOf<String>()
    private val autoresDisponibles = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hayConexion()) {
            val intent = Intent(this, SinInternetActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_guardados)
        loadingSpinner = findViewById(R.id.loadingSpinner)
        setupBottomNavigation(R.id.nav_Guardados)

        recyclerView = findViewById(R.id.recetasRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecetaAdapter(this, recetas)
        recyclerView.adapter = adapter


        val searchInput = findViewById<TextInputEditText>(R.id.searchInput)
        searchInput.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search = searchInput.text.toString().trim()
                fetchRecetasGuardadas()
                return@OnEditorActionListener true
            }
            false
        })


        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val nuevoTexto = s?.toString()?.trim() ?: ""
                if (nuevoTexto != search) {
                    search = nuevoTexto
                    fetchRecetasGuardadas()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val chipInclude = findViewById<Chip>(R.id.chipInclude)
        val ingredientesSeleccionados = mutableSetOf<String>()
        val chipExclude = findViewById<Chip>(R.id.chipExclude)
        val ingredientesAExcluir = mutableSetOf<String>()
        val chipOrder = findViewById<Chip>(R.id.chipOrder)
        val chipType = findViewById<Chip>(R.id.chipType)
        val chipAuthor = findViewById<Chip>(R.id.chipAuthor)
        val btnFiltros = findViewById<ImageButton>(R.id.btnFiltros)
        val filtersContainer = findViewById<LinearLayout>(R.id.filtersContainer)

        btnFiltros.setOnClickListener {
            if (filtersContainer.visibility == View.GONE) {
                filtersContainer.visibility = View.VISIBLE
            } else {
                filtersContainer.visibility = View.GONE
            }
        }



        // CHIP ORDER
        chipOrder.setOnClickListener {
            val popup = PopupMenu(this, chipOrder)
            popup.menu.add("Más reciente")
            popup.menu.add("Más antiguo")
            popup.menu.add("Alfabéticamente")
            popup.menu.add("Nombre de usuario")

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
                    "Nombre de usuario" -> {
                        sortBy = "username"
                        sortOrder = "asc"
                        chipOrder.text = "Ordenado por: usuario"
                    }
                }
                fetchRecetasGuardadas()
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

            val tiposArray = tiposDisponibles.sortedBy { it.lowercase() }.toTypedArray()
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
                fetchRecetasGuardadas()
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

// CHIP AUTHOR
        val autoresSeleccionados = mutableSetOf<String>()
        chipAuthor.setOnClickListener {
            if (autoresDisponibles.isEmpty()) {
                Toast.makeText(this, "Todavía no se cargaron los usuarios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val autoresArray = autoresDisponibles.sortedBy { it.lowercase() }.toTypedArray()
            val seleccionadosTemp = BooleanArray(autoresArray.size) { i ->
                autoresSeleccionados.contains(autoresArray[i])
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Seleccionar autores")
            builder.setMultiChoiceItems(autoresArray, seleccionadosTemp) { _, which, isChecked ->
                if (isChecked) {
                    autoresSeleccionados.add(autoresArray[which])
                } else {
                    autoresSeleccionados.remove(autoresArray[which])
                }
            }

            builder.setPositiveButton("Aplicar") { _, _ ->
                author = autoresSeleccionados.joinToString(",")
                chipAuthor.text = if (autoresSeleccionados.isEmpty()) {
                    "Autores: todos"
                } else {
                    "Autores: ${autoresSeleccionados.joinToString(", ")}"
                }
                fetchRecetasGuardadas()
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

        chipInclude.setOnClickListener {
            if (ingredientesDisponibles.isEmpty()) {
                Toast.makeText(this, "Todavía no se cargaron los ingredientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ingredientesArray = ingredientesDisponibles.sorted().toTypedArray()
            val seleccionadosTemp = BooleanArray(ingredientesArray.size) { i ->
                ingredientesSeleccionados.contains(ingredientesArray[i])
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Seleccionar ingredientes a incluir")
            builder.setMultiChoiceItems(ingredientesArray, seleccionadosTemp) { _, which, isChecked ->
                if (isChecked) {
                    ingredientesSeleccionados.add(ingredientesArray[which])
                } else {
                    ingredientesSeleccionados.remove(ingredientesArray[which])
                }
            }

            builder.setPositiveButton("Aplicar") { _, _ ->
                include = ingredientesSeleccionados.joinToString(",")
                chipInclude.text = if (ingredientesSeleccionados.isEmpty()) {
                    "Incluir: todo"
                } else {
                    "Incluir: ${ingredientesSeleccionados.joinToString(", ")}"
                }
                fetchRecetasGuardadas()
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

        chipExclude.setOnClickListener {
            if (ingredientesDisponibles.isEmpty()) {
                Toast.makeText(this, "Todavía no se cargaron los ingredientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ingredientesArray = ingredientesDisponibles.sorted().toTypedArray()
            val seleccionadosTemp = BooleanArray(ingredientesArray.size) { i ->
                ingredientesAExcluir.contains(ingredientesArray[i])
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Seleccionar ingredientes a excluir")
            builder.setMultiChoiceItems(ingredientesArray, seleccionadosTemp) { _, which, isChecked ->
                if (isChecked) {
                    ingredientesAExcluir.add(ingredientesArray[which])
                } else {
                    ingredientesAExcluir.remove(ingredientesArray[which])
                }
            }

            builder.setPositiveButton("Aplicar") { _, _ ->
                exclude = ingredientesAExcluir.joinToString(",")
                chipExclude.text = if (ingredientesAExcluir.isEmpty()) {
                    "Excluir: nada"
                } else {
                    "Excluir: ${ingredientesAExcluir.joinToString(", ")}"
                }
                fetchRecetasGuardadas()
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

        cargarIngredientesGlobales()
        fetchRecetasGuardadas()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            fetchRecetasGuardadas()
        }
    }

    private fun fetchRecetasGuardadas() {
        val client = OkHttpClient()
        runOnUiThread {
            loadingSpinner.visibility = View.VISIBLE
            recyclerView.visibility = View.INVISIBLE
        }
        val urlBuilder = "https://desarrolloitpoapi.onrender.com/api/recipes"
            .toHttpUrlOrNull()!!.newBuilder()

        urlBuilder.addQueryParameter("savedByUser", "true")
        urlBuilder.addQueryParameter("ingredient", include)
        urlBuilder.addQueryParameter("excludeIngredient", exclude)
        urlBuilder.addQueryParameter("name", search)

        if (type != "all") {
            urlBuilder.addQueryParameter("classification", type)
        }

        if (author.isNotEmpty()) {
            urlBuilder.addQueryParameter("createdBy", author)
        }

        urlBuilder.addQueryParameter("sortBy", sortBy)
        urlBuilder.addQueryParameter("sortOrder", sortOrder)

        val token = TokenUtils.obtenerToken(this)
        Log.d("TOKEN_CHECK", "Token actual: $token")

        val requestBuilder = Request.Builder()
            .url(urlBuilder.build())
            .get()

        if (token.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    Toast.makeText(this@GuardadosActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonBody = response.body?.string() ?: return
                val jsonObject = JSONObject(jsonBody)

                if (!jsonObject.has("recipes")) {
                    Log.e("CRASH_DEBUG", "No vino 'recipes'. Respuesta: $jsonBody")
                    return
                }

                val jsonArray = jsonObject.getJSONArray("recipes")
                recetas.clear()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    if (!item.optBoolean("status", false)) continue

                    val name = item.getString("name")
                    val classification = item.getString("classification")
                    val description = item.optString("description", "")


                    tiposDisponibles.add(classification)
                    val username = item.optString("username", "Desconocido")
                    autoresDisponibles.add(username)

                    val ingredients = mutableListOf<String>()
                    val ingredientsList = item.optJSONArray("ingredients")
                    if (ingredientsList != null) {
                        for (j in 0 until ingredientsList.length()) {
                            val ing = ingredientsList.getJSONObject(j).getString("name")
                            ingredients.add(ing)
                            ingredientesDisponibles.add(ing)
                        }
                    }

                    val steps = item.optJSONArray("steps") ?: JSONArray()
                    val image = item.optJSONArray("frontpagePhotos")?.optString(0) ?: ""

                    val id = item.getString("_id")
                    val uploadDate = item.optString("uploadDate", "")
                    val rating = item.optDouble("rating", 0.0)
                    val portions = item.optInt("portions", 1)
                    val stepsJson = item.optJSONArray("steps")?.toString() ?: "[]"
                    val ingredientsJson = ingredientsList.toString()

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
                        rating = rating,
                        portions = portions,
                        stepsJson = stepsJson,
                        ingredientsJson = ingredientsJson
                    )

                    recetas.add(receta)
                }

                runOnUiThread {
                    adapter.notifyDataSetChanged()
                    loadingSpinner.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun cargarIngredientesGlobales() {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes"
            .toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("sortBy", "uploadDate")
            .addQueryParameter("sortOrder", "desc")
            .build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val jsonBody = response.body?.string() ?: return
                val jsonObject = JSONObject(jsonBody)

                if (!jsonObject.has("recipes")) {
                    Log.e("CRASH_DEBUG", "No vino 'recipes'. Respuesta: $jsonBody")
                    return
                }

                val jsonArray = jsonObject.getJSONArray("recipes")
                for (i in 0 until jsonArray.length()) {
                    val receta = jsonArray.getJSONObject(i)

                    // 💥 ACÁ se agregan tipos y autores
                    tiposDisponibles.add(receta.getString("classification"))

                    val username = receta.optString("username", "")
                    if (username.isNotEmpty()) {
                        autoresDisponibles.add(username)
                    }

                    val ingredientsList = receta.optJSONArray("ingredients") ?: continue
                    for (j in 0 until ingredientsList.length()) {
                        val ing = ingredientsList.getJSONObject(j)
                        ingredientesDisponibles.add(ing.getString("name"))
                    }
                }
            }
        })
    }

    private fun hayConexion(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}