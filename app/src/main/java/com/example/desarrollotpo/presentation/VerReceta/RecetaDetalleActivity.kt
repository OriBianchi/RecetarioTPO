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

    private fun formatearFecha(isoDateString: String): String {
        return try {
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US)
            isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

            // Reemplazamos +00:00 por +0000 para compatibilidad con el patrón Z
            val fixed = isoDateString.replace("+00:00", "+0000")
            val date = isoFormat.parse(fixed)

            val ahora = java.util.Calendar.getInstance()
            val añoActual = ahora.get(java.util.Calendar.YEAR)

            val fecha = java.util.Calendar.getInstance().apply { time = date!! }
            val añoDeLaFecha = fecha.get(java.util.Calendar.YEAR)

            val displayFormat = if (añoActual == añoDeLaFecha) {
                java.text.SimpleDateFormat("dd 'de' MMMM", java.util.Locale("es", "ES"))
            } else {
                java.text.SimpleDateFormat("dd 'de' MMMM ''yy", java.util.Locale("es", "ES"))
            }

            displayFormat.format(date)
        } catch (e: Exception) {
            "Fecha desconocida"
        }
    }


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

            if (rating < 1 || rating > 5) {
                Toast.makeText(this, "Seleccioná una calificación de 1 a 5 estrellas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            enviarComentario(textoComentario, rating)

            runOnUiThread {
                etComentario.text.clear()
                ratingBar.rating = 0f
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
                val uploadDate = json.optString("uploadDate", "")
                val fechaFormateada = formatearFecha(uploadDate)

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

                val comentarios = mutableListOf<Comentario>()
                json.optJSONArray("comments")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val com = arr.getJSONObject(i)
                        val autor = com.optString("username", "Anon")
                        val texto = com.optString("text", "")
                        val rating = com.optDouble("rating", 0.0)
                        comentarios.add(Comentario(autor, texto, rating))
                    }
                }

                runOnUiThread {
                    actualizarIcono()
                    findViewById<TextView>(R.id.tvTituloReceta).text = titulo
                    findViewById<TextView>(R.id.tvAutor).text = "Por @$autor"
                    findViewById<TextView>(R.id.tvDescripcion).text = descripcion
                    findViewById<TextView>(R.id.tvCantidadPorcion).text = cantidadPorcion.toString()
                    findViewById<TextView>(R.id.tvFecha).text = "📅 $fechaFormateada"
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

    private fun mostrarComentarios(lista: List<Comentario>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorComentarios)
        contenedor.removeAllViews()

        val inflater = layoutInflater
        lista.forEach { comentario ->
            val view = inflater.inflate(R.layout.item_comentario, contenedor, false)

            val autorView = view.findViewById<TextView>(R.id.autorComentario)
            val textoView = view.findViewById<TextView>(R.id.textoComentario)
            val ratingView = view.findViewById<TextView>(R.id.ratingComentario)

            autorView.text = comentario.autor
            textoView.text = comentario.texto
            ratingView.text = "⭐ %.1f".format(comentario.rating)

            contenedor.addView(view)
        }
    }

    private fun enviarComentario(texto: String, rating: Int) {
        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/$recetaId/comment"
        val token = TokenUtils.obtenerToken(this)

        val jsonBody = """{
        "text": "${texto.replace("\"", "\\\"")}",
        "rating": $rating
    }"""

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