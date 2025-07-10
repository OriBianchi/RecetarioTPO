package com.example.desarrollotpo.presentation.VerReceta

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.compose.ui.unit.dp
import com.example.desarrollotpo.R
import com.example.desarrollotpo.core.BaseActivity
import com.example.desarrollotpo.utils.TokenUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import android.view.View


class RecetaDetalleActivity : BaseActivity() {

    private lateinit var btnGuardar: ImageButton
    private lateinit var recetaId: String
    private lateinit var loaderOverlay: View
    private val ingredientesOriginales = mutableListOf<Ingrediente>()
    private var cantidadPorcion = 1
    private var isSaved = false
    private var porcionesOriginales = 1
    private var userRole: String = "user"

    private fun formatearFecha(isoDateString: String): String {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US)
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = isoFormat.parse(isoDateString) ?: return "Fecha desconocida"

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.time = date

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val recipeYear = calendar.get(Calendar.YEAR)

            val displayFormat = if (recipeYear == currentYear) {
                SimpleDateFormat("d 'de' MMMM", Locale("es", "ES"))
            } else {
                SimpleDateFormat("d 'de' MMMM 'del' yy", Locale("es", "ES"))
            }

            displayFormat.format(date)
        } catch (e: Exception) {
            "Fecha desconocida"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receta_detalle)

        loaderOverlay = findViewById(R.id.loaderOverlay)
        mostrarLoader()  // <-- lo mostramos ni bien empieza
        recetaId = intent.getStringExtra("RECIPE_ID") ?: ""
        isSaved = intent.getBooleanExtra("IS_SAVED", false)
        btnGuardar = findViewById(R.id.btnGuardar)
        actualizarIcono()

        verificarRolYcargarReceta()

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
                val rating = json.optDouble("rating", 0.0)
                val isApproved = json.optBoolean("status", false)
                porcionesOriginales = json.optInt("portions", 1)
                cantidadPorcion = porcionesOriginales

                ingredientesOriginales.clear()
                json.optJSONArray("ingredients")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val ing = arr.getJSONObject(i)
                        val name = ing.optString("name", "")
                        val amount = ing.optDouble("amount", 0.0)
                        val unit = ing.optString("unit", "g") // fallback a gramos si falta
                        ingredientesOriginales.add(Ingrediente(name, amount, unit))
                    }
                }

                val pasos = mutableListOf<Pair<String, List<String>>>()
                json.optJSONArray("steps")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val stepJson = arr.getJSONObject(i)
                        val description = stepJson.optString("description", "")
                        val photos = mutableListOf<String>()
                        stepJson.optJSONArray("photos")?.let { photosArray ->
                            for (j in 0 until photosArray.length()) {
                                val photo = photosArray.getJSONObject(j).optString("base64", "")
                                if (photo.isNotEmpty()) photos.add(photo)
                            }
                        }
                        pasos.add(Pair(description, photos))
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
                    findViewById<TextView>(R.id.tvRating).text = "⭐ %.1f".format(rating)

                    val pendienteView = findViewById<TextView>(R.id.tvPendienteAprobacion)
                    if (isApproved) {
                        pendienteView.visibility = View.GONE
                    } else {
                        pendienteView.visibility = View.VISIBLE
                    }

                    val seccionComentarios = findViewById<LinearLayout>(R.id.seccionComentarios)
                    if (isApproved) {
                        seccionComentarios.visibility = View.VISIBLE
                    } else {
                        seccionComentarios.visibility = View.GONE
                    }


                    // Renderizar imagen principal
                    val imageView = findViewById<ImageView>(R.id.imageReceta)
                    val frontImage = json.optJSONArray("frontpagePhotos")?.optJSONObject(0)?.optString("base64", "")

                    if (!frontImage.isNullOrEmpty() && frontImage.contains("base64,")) {
                        try {
                            val base64Clean = frontImage.substringAfter("base64,").trim()
                            val imageBytes = android.util.Base64.decode(base64Clean, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap)
                            } else {
                                imageView.setImageResource(R.drawable.placeholder)
                            }
                        } catch (e: Exception) {
                            imageView.setImageResource(R.drawable.placeholder)
                        }
                    } else {
                        imageView.setImageResource(R.drawable.placeholder)
                    }

                    actualizarIngredientes()
                    mostrarPasos(pasos)
                    mostrarComentarios(comentarios)

                    val layoutModeracion = findViewById<LinearLayout>(R.id.layoutModeracionAdmin)
                    val btnAprobar = findViewById<Button>(R.id.btnAprobarAdmin)
                    val btnRechazar = findViewById<Button>(R.id.btnRechazarAdmin)

                    if (userRole == "admin" && !isApproved) {
                        layoutModeracion.visibility = View.VISIBLE

                        btnAprobar.setOnClickListener {
                            aprobarReceta(recetaId)
                        }

                        btnRechazar.setOnClickListener {
                            rechazarReceta(recetaId)
                        }
                    } else {
                        layoutModeracion.visibility = View.GONE
                    }


                    ocultarLoader()
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


    private fun actualizarIngredientes() {
        val nuevos = ingredientesOriginales.map {
            Ingrediente(
                it.name,
                it.amount * cantidadPorcion / porcionesOriginales,
                it.unit
            )
        }
        mostrarIngredientes(nuevos)
    }

    private fun mostrarIngredientes(lista: List<Ingrediente>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorIngredientes)
        contenedor.removeAllViews()

        val inflater = layoutInflater
        for (item in lista) {
            val view = inflater.inflate(R.layout.item_ingrediente, contenedor, false)

            val nombre = view.findViewById<TextView>(R.id.nombreIngrediente)
            val cantidad = view.findViewById<TextView>(R.id.cantidadIngrediente)
            val unidad = view.findViewById<TextView>(R.id.medidaIngrediente)

            nombre.text = item.name
            cantidad.text = String.format("%.2f", item.amount).trimEnd('0').trimEnd('.')
            unidad.text = item.unit

            contenedor.addView(view)
        }
    }

    private fun mostrarPasos(lista: List<Pair<String, List<String>>>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorPasos)
        contenedor.removeAllViews()
        val inflater = layoutInflater

        lista.forEachIndexed { index, pasoData ->
            val (descripcionPaso, imagenesBase64) = pasoData
            val view = inflater.inflate(R.layout.item_paso, contenedor, false)

            val titulo = view.findViewById<TextView>(R.id.tituloPaso)
            val descripcion = view.findViewById<TextView>(R.id.descripcionPaso)
            val contenedorImagenes = view.findViewById<LinearLayout>(R.id.contenedorImagenesPaso)

            titulo.text = "Paso ${index + 1}"
            descripcion.text = descripcionPaso

            // Renderizar imágenes base64
            contenedorImagenes.removeAllViews()
            imagenesBase64.forEach { base64 ->
                val imageView = ImageView(this)
                val base64Clean = base64.substringAfter("base64,", "")
                if (base64Clean.isNotEmpty()) {
                    try {
                        val bytes = android.util.Base64.decode(base64Clean, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        imageView.setImageBitmap(bitmap)
                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        layoutParams.topMargin = 8.dp // agregamos margen de 8dp arriba
                        imageView.layoutParams = layoutParams
                        imageView.adjustViewBounds = true
                        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                        contenedorImagenes.addView(imageView)
                    } catch (e: Exception) {
                        // Podés loguearlo si querés
                    }
                }
            }

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
                        mostrarDialogoComentarioEnviado()
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

    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun actualizarIcono() {
        if (isSaved) {
            btnGuardar.setImageResource(R.drawable.baseline_bookmark_24)
        } else {
            btnGuardar.setImageResource(R.drawable.baseline_bookmark_border_24)
        }
    }

    private fun aprobarReceta(recipeId: String) {
        mostrarLoader()

        val token = TokenUtils.obtenerToken(this)
        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/recipes/$recipeId/approve")
            .patch(RequestBody.create(null, ByteArray(0)))
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    ocultarLoader()
                    Toast.makeText(this@RecetaDetalleActivity, "Error al aprobar receta", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    ocultarLoader()
                    if (response.isSuccessful) {
                        Toast.makeText(this@RecetaDetalleActivity, "Receta aprobada", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@RecetaDetalleActivity, "Error al aprobar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun rechazarReceta(recipeId: String) {
        mostrarLoader()

        val token = TokenUtils.obtenerToken(this)
        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/recipes/$recipeId/reject")
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    ocultarLoader()
                    Toast.makeText(this@RecetaDetalleActivity, "Error al rechazar receta", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    ocultarLoader()
                    if (response.isSuccessful) {
                        Toast.makeText(this@RecetaDetalleActivity, "Receta rechazada", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@RecetaDetalleActivity, "Error al rechazar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun verificarRolYcargarReceta() {
        val token = TokenUtils.obtenerToken(this)
        if (token.isEmpty()) {
            cargarReceta() // sin token, seguimos sin rol
            return
        }

        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/auth/me")
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RecetaDetalleActivity, "Error al verificar rol", Toast.LENGTH_SHORT).show()
                    cargarReceta()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    userRole = json.optString("role", "user")
                }
                runOnUiThread {
                    cargarReceta()
                }
            }
        })
    }
    private fun mostrarDialogoComentarioEnviado() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Comentario enviado")
            .setMessage("Tu comentario fue enviado correctamente. Deberá ser aprobado por los administradores antes de que sea visible para otros usuarios.")
            .setPositiveButton("OK") { _, _ ->
                // Acción tras confirmar: podés solo cerrar o recargar comentarios
                finish()
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }



}