package com.example.desarrollotpo.presentation.VerReceta

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import com.example.desarrollotpo.R
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.utils.TokenUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RecetaDetalleActivity : BaseActivity() {

    private lateinit var btnGuardar: ImageButton
    private lateinit var recetaId: String

    private val ingredientesOriginales = mutableListOf<Pair<String, Int>>()
    private var cantidadPorcion = 1
    private var isSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receta_detalle)

        recetaId = intent.getStringExtra("RECIPE_ID") ?: ""
        isSaved = intent.getBooleanExtra("IS_SAVED", false)
        btnGuardar = findViewById(R.id.btnGuardar)
        actualizarIcono()

        cargarReceta()

        val btnBack = findViewById<ImageView>(R.id.backInicio)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val etComentario = findViewById<EditText>(R.id.etComentario)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val btnEnviarComentario = findViewById<Button>(R.id.btnEnviarComentario)

        btnEnviarComentario.setOnClickListener {
            val textoComentario = etComentario.text.toString().trim()
            val rating = ratingBar.rating.toInt()

            if (textoComentario.isEmpty()) {
                Toast.makeText(this, "Escribí un comentario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            enviarComentario(textoComentario)

            if (rating > 0) {
                enviarRating(rating)
            }

            runOnUiThread {
                etComentario.text.clear()
                ratingBar.rating = 0f
                Toast.makeText(this, "Comentario enviado!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnMenosPorcion = findViewById<Button>(R.id.btnMenosPorcion)
        val btnMasPorcion = findViewById<Button>(R.id.btnMasPorcion)
        val tvCantidadPorcion = findViewById<TextView>(R.id.tvCantidadPorcion)

        btnMenosPorcion.setOnClickListener {
            if (cantidadPorcion > 1) {
                cantidadPorcion--
                tvCantidadPorcion.text = cantidadPorcion.toString()
                actualizarIngredientes()
            }
        }

        btnMasPorcion.setOnClickListener {
            cantidadPorcion++
            tvCantidadPorcion.text = cantidadPorcion.toString()
            actualizarIngredientes()
        }

        btnGuardar.setOnClickListener {
            if (isSaved) {
                desguardarReceta()
            } else {
                guardarReceta()
            }
        }
    }

    private fun cargarReceta() {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/$recetaId"

        val token = TokenUtils.obtenerToken(this)
        val request = Request.Builder().url(url).get()
        if (token.isNotEmpty()) {
            request.header("Authorization", "Bearer $token")
        }

        client.newCall(request.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RecetaDetalleActivity, "Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonBody = response.body?.string() ?: return
                val json = JSONObject(jsonBody)

                val titulo = json.optString("name", "")
                val autor = json.optJSONObject("userId")?.optString("username") ?: ""
                val descripcion = json.optString("description", "")

                ingredientesOriginales.clear()
                json.optJSONArray("ingredients")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val ing = arr.getJSONObject(i)
                        ingredientesOriginales.add(Pair(ing.getString("name"), ing.optInt("quantity", 1)))
                    }
                }

                val pasos = mutableListOf<String>()
                json.optJSONArray("steps")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        pasos.add(arr.getJSONObject(i).optString("description", ""))
                    }
                }

                val comentarios = mutableListOf<Pair<String, String>>()
                json.optJSONArray("comments")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val com = arr.getJSONObject(i)
                        val autorCom = com.optString("username") ?: "Anon"
                        comentarios.add(Pair(autorCom, com.optString("text", "")))
                    }
                }

                runOnUiThread {
                    actualizarIcono()
                    findViewById<TextView>(R.id.tvTituloReceta).text = titulo
                    findViewById<TextView>(R.id.tvAutor).text = "Por @$autor"
                    findViewById<TextView>(R.id.tvDescripcion).text = descripcion
                    findViewById<TextView>(R.id.tvCantidadPorcion).text = cantidadPorcion.toString()
                    actualizarIngredientes()
                    mostrarPasos(pasos)
                    mostrarComentarios(comentarios)
                }
            }
        })
    }

    private fun actualizarIngredientes() {
        val nuevos = ingredientesOriginales.map { Pair(it.first, "${it.second * cantidadPorcion}g") }
        mostrarIngredientes(nuevos)
    }

    private fun mostrarIngredientes(lista: List<Pair<String, String>>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorIngredientes)
        contenedor.removeAllViews()

        val inflater = layoutInflater
        for (item in lista) {
            val view = inflater.inflate(R.layout.item_ingrediente, contenedor, false)

            val nombre = view.findViewById<TextView>(R.id.nombreIngrediente)
            val cantidad = view.findViewById<TextView>(R.id.cantidadIngrediente)

            nombre.text = item.first
            cantidad.text = item.second

            contenedor.addView(view)
        }
    }

    private fun mostrarPasos(lista: List<String>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorPasos)
        contenedor.removeAllViews()

        val inflater = layoutInflater
        lista.forEachIndexed { index, paso ->
            val view = inflater.inflate(R.layout.item_paso, contenedor, false)

            val titulo = view.findViewById<TextView>(R.id.tituloPaso)
            val descripcion = view.findViewById<TextView>(R.id.descripcionPaso)

            titulo.text = "Paso ${index + 1}"
            descripcion.text = paso

            contenedor.addView(view)
        }
    }

    private fun mostrarComentarios(lista: List<Pair<String, String>>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorComentarios)
        contenedor.removeAllViews()

        val inflater = layoutInflater
        lista.forEach { (autor, texto) ->
            val view = inflater.inflate(R.layout.item_comentario, contenedor, false)

            val autorView = view.findViewById<TextView>(R.id.autorComentario)
            val textoView = view.findViewById<TextView>(R.id.textoComentario)

            autorView.text = autor
            textoView.text = texto

            contenedor.addView(view)
        }
    }

    private fun enviarComentario(texto: String) {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/$recetaId/comment"
        val token = TokenUtils.obtenerToken(this)

        val jsonBody = """{"text": "$texto"}"""
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RecetaDetalleActivity, "Error al comentar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RecetaDetalleActivity, "Comentario enviado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@RecetaDetalleActivity, "Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun enviarRating(rating: Int) {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/$recetaId/rate"
        val token = TokenUtils.obtenerToken(this)

        val jsonBody = """{"rating": $rating}"""
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RecetaDetalleActivity, "Error al calificar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RecetaDetalleActivity, "Calificación enviada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@RecetaDetalleActivity, "Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun guardarReceta() {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/user/recipes/save"
        val token = TokenUtils.obtenerToken(this)
        val body = FormBody.Builder().add("recipeId", recetaId).build()
        val request = Request.Builder().url(url).post(body)
        if (token.isNotEmpty()) {
            request.header("Authorization", "Bearer $token")
        }

        client.newCall(request.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RecetaDetalleActivity, "Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                isSaved = true
                runOnUiThread {
                    actualizarIcono()
                    Toast.makeText(this@RecetaDetalleActivity, "Guardada", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun desguardarReceta() {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/user/recipes/unsave"
        val token = TokenUtils.obtenerToken(this)
        val body = FormBody.Builder().add("recipeId", recetaId).build()
        val request = Request.Builder().url(url).post(body)
        if (token.isNotEmpty()) {
            request.header("Authorization", "Bearer $token")
        }

        client.newCall(request.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RecetaDetalleActivity, "Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                isSaved = false
                runOnUiThread {
                    actualizarIcono()
                    Toast.makeText(this@RecetaDetalleActivity, "Quitada de guardadas", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun actualizarIcono() {
        if (isSaved) {
            btnGuardar.setImageResource(R.drawable.baseline_bookmark_24)
        } else {
            btnGuardar.setImageResource(R.drawable.baseline_bookmark_border_24)
        }
    }
}