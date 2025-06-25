package com.example.desarrollotpo.presentation.home

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.presentation.common.SinInternetActivity
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.widget.PopupMenu
import android.text.Editable
import android.text.TextWatcher
import com.example.desarrollotpo.utils.setupBottomNavigation





class InicioActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecetaAdapter
    private val recetas = mutableListOf<Receta>()
    private val autoresDisponibles = mutableSetOf<String>()
    private val ingredientesDisponibles = mutableSetOf<String>()


    // Filtros
    private var sortBy = "uploadDate"
    private var sortOrder = "desc"
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
        setupBottomNavigation(R.id.nav_inicio)


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
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val nuevoTexto = s?.toString()?.trim() ?: ""
                if (nuevoTexto != search) {
                    search = nuevoTexto
                    fetchRecetas()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })



        val chipOrder = findViewById<Chip>(R.id.chipOrder)
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
                fetchRecetas()
                true
            }
            popup.show()
        }
        val chipInclude = findViewById<Chip>(R.id.chipInclude)
        val ingredientesSeleccionados = mutableSetOf<String>()
        val chipExclude = findViewById<Chip>(R.id.chipExclude)
        val ingredientesAExcluir = mutableSetOf<String>()
        val chipAuthor = findViewById<Chip>(R.id.chipAuthor)
        val autoresSeleccionados = mutableSetOf<String>()



        chipInclude.setOnClickListener {
            if (ingredientesDisponibles.isEmpty()) {
                Toast.makeText(this, "Todavía no se cargaron los ingredientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ingredientesArray = ingredientesDisponibles.sorted().toTypedArray()
            val seleccionadosTemp = BooleanArray(ingredientesArray.size) { i ->
                ingredientesSeleccionados.contains(ingredientesArray[i])
            }

            val builder = android.app.AlertDialog.Builder(this)
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
                fetchRecetas()
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

            val builder = android.app.AlertDialog.Builder(this)
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
                fetchRecetas()
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }
        chipAuthor.setOnClickListener {
            if (autoresDisponibles.isEmpty()) {
                Toast.makeText(this, "Todavía no se cargaron los usuarios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val autoresArray = autoresDisponibles.sorted().toTypedArray()
            val seleccionadosTemp = BooleanArray(autoresArray.size) { i ->
                autoresSeleccionados.contains(autoresArray[i])
            }

            val builder = android.app.AlertDialog.Builder(this)
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
                fetchRecetas()
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }




        cargarIngredientesGlobales()
        fetchRecetas()
    }

    private fun fetchRecetas() {
        val client = OkHttpClient()

        val urlBuilder = "https://desarrolloitpoapi.onrender.com/api/recipes"
            .toHttpUrlOrNull()!!.newBuilder()

        urlBuilder.addQueryParameter("sortBy", sortBy)
        urlBuilder.addQueryParameter("sortOrder", sortOrder)
        urlBuilder.addQueryParameter("type", type)
        urlBuilder.addQueryParameter("ingredient", include)
        urlBuilder.addQueryParameter("excludeIngredient", exclude)
        urlBuilder.addQueryParameter("createdBy", author)
        urlBuilder.addQueryParameter("savedByUser", if (saved == "only") "true" else "")
        urlBuilder.addQueryParameter("name", search)



        val requestUrl = urlBuilder.build()
        Log.d("DEBUG_API", requestUrl.toString())


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
                            val nombre = ing.getString("name") // obtenemos el nombre
                            ingredients.add(nombre) // lo agregamos a la lista de esta receta
                            ingredientesDisponibles.add(nombre) // lo agregamos al set global
                        }
                    }

                    val user = item.optJSONObject("userId")
                    val username = user?.optString("username") ?: "Desconocido"
                    autoresDisponibles.add(username)
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
            override fun onFailure(call: Call, e: IOException) {
                // No hagas nada o poné un log si querés
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonBody = response.body?.string() ?: return
                val jsonObject = JSONObject(jsonBody)
                val jsonArray = jsonObject.getJSONArray("recipes")

                val nuevosIngredientes = mutableSetOf<String>()

                for (i in 0 until jsonArray.length()) {
                    val receta = jsonArray.getJSONObject(i)
                    val ingredientsList = receta.optJSONArray("ingredients") ?: continue

                    for (j in 0 until ingredientsList.length()) {
                        val ing = ingredientsList.getJSONObject(j)
                        nuevosIngredientes.add(ing.getString("name"))
                    }
                }


                ingredientesDisponibles.addAll(nuevosIngredientes)
            }
        })
    }


}
