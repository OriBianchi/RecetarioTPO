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

class GuardadosActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecetaAdapter
    private lateinit var loadingSpinner: ProgressBar
    private val recetas = mutableListOf<Receta>()
    private val ingredientesDisponibles = mutableSetOf<String>()
    private var search = ""
    private var include = ""
    private var exclude = ""

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
                    val name = item.getString("name")
                    val classification = item.getString("classification")
                    val description = item.optString("description", "")
                    val ingredients = mutableListOf<String>()
                    val ingredientsList = item.optJSONArray("ingredients")
                    if (ingredientsList != null) {
                        for (j in 0 until ingredientsList.length()) {
                            val ing = ingredientsList.getJSONObject(j).getString("name")
                            ingredients.add(ing)
                            ingredientesDisponibles.add(ing)
                        }
                    }
                    val username = item.optString("username", "Desconocido")
                    val steps = item.optJSONArray("steps") ?: JSONArray()
                    val image = item.optJSONArray("frontpagePhotos")?.optString(0) ?: ""

                    val id = item.getString("_id")
                    val receta = Receta(
                        id = id,
                        name = name,
                        classification = classification,
                        ingredients = ingredients,
                        description = description,
                        frontImage = image,
                        author = username,
                        stepsCount = steps.length(),
                        isSaved = true,
                        status = item.optBoolean("status", false)
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