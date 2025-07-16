package com.example.desarrollotpo.presentation.crear

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.desarrollotpo.R
import com.example.desarrollotpo.utils.setupBottomNavigation
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import okhttp3.*
import org.json.JSONArray
import androidx.core.graphics.drawable.toBitmap
import com.example.desarrollotpo.utils.TokenUtils


class CrearActivity : AppCompatActivity() {

    private lateinit var tvPorciones: TextView
    private lateinit var btnIncrementar: Button
    private lateinit var btnDecrementar: Button
    private lateinit var tvTipoSeleccionado: TextView
    private lateinit var contenedorIngredientes: LinearLayout
    private lateinit var contenedorPasos: LinearLayout
    private lateinit var btnElegirFotos: Button
    private lateinit var contenedorMiniaturas: LinearLayout
    private lateinit var tvCantidadFotos: TextView
    private lateinit var iconoErrorFoto: ImageView
    private lateinit var loaderOverlay: View


    private var porciones: Int = 1
    private val medidas = listOf("g", "kg", "unidades", "tazas", "ml", "cucharadas", "cucharaditas", "pizca", "litros", "cc")
    private val categorias = listOf("Desayuno", "Almuerzo", "Cena", "Merienda", "Snack", "Vegano", "Vegetariano", "Sin TACC", "Otro")
    private val imagenesBase64 = mutableListOf<String>()
    private val fotosPasosBase64 = mutableListOf<MutableList<String>>()
    private var pasoViewSeleccionadoParaImagen: View? = null

    private var recetaNombreOriginal: String? = null


    private val launcherImagenesPaso = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        pasoViewSeleccionadoParaImagen?.let { pasoView ->
            val contenedorImagenes = pasoView.findViewById<LinearLayout>(R.id.contenedorImagenesPaso)
            val tvCantidad = pasoView.findViewById<TextView>(R.id.tvCantidadImagenesPaso)

            contenedorImagenes.removeAllViews()
            val indexPaso = contenedorPasos.indexOfChild(pasoView)
            if (indexPaso >= fotosPasosBase64.size) {
                fotosPasosBase64.add(mutableListOf())
            } else {
                fotosPasosBase64[indexPaso] = mutableListOf()
            }

            for (uri in uris) {
                val bitmap = uriToBitmap(uri)
                val compressed = compressBitmap(bitmap, 90)
                val base64 = bitmapToBase64(compressed)

                fotosPasosBase64[indexPaso].add(base64) // guarda base64

                val imageView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                        setMargins(8, 8, 8, 8)
                    }
                    setImageBitmap(compressed)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                contenedorImagenes.addView(imageView)
            }
        }
    }


    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imagenesBase64.clear()
        contenedorMiniaturas.removeAllViews()

        uri?.let {
            val bitmap = uriToBitmap(it)
            val compressed = compressBitmap(bitmap, 90)

            try {
                val base64 = bitmapToBase64(compressed)
                imagenesBase64.add(base64)

                val imageView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                        setMargins(8, 8, 8, 8)
                    }
                    setImageBitmap(compressed)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                contenedorMiniaturas.addView(imageView)

            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "La imagen seleccionada supera los 3MB", Toast.LENGTH_LONG).show()
            }
        }

        tvCantidadFotos.text = "(${imagenesBase64.size} seleccionada)"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_receta)
        tvPorciones = findViewById(R.id.tvPorciones)
        btnIncrementar = findViewById(R.id.btnIncrementarPorciones)
        btnDecrementar = findViewById(R.id.btnDecrementarPorciones)
        contenedorIngredientes = findViewById(R.id.contenedorIngredientes)
        contenedorPasos = findViewById(R.id.contenedorPasos)
        tvTipoSeleccionado = findViewById(R.id.tvTipoSeleccionado)
        btnElegirFotos = findViewById(R.id.btnElegirFotosPortada)
        tvCantidadFotos = findViewById(R.id.tvCantidadFotos)
        contenedorMiniaturas = findViewById(R.id.contenedorMiniaturas)
        iconoErrorFoto = findViewById(R.id.iconoErrorFoto)
        loaderOverlay = findViewById(R.id.loaderOverlay)
        val recetaId = intent.getStringExtra("RECIPE_ID")
        val recetaPortions = intent.getIntExtra("RECIPE_PORTIONS", 1)
        val recetaFrontImage = intent.getStringExtra("RECIPE_FRONT_IMAGE")
        val recetaStepsJson = intent.getStringExtra("RECIPE_STEPS_JSON")
        val recetaNombre = intent.getStringExtra("RECIPE_NAME")
        recetaNombreOriginal = recetaNombre

        val recetaDescripcion = intent.getStringExtra("RECIPE_DESCRIPTION")
        val recetaTipo = intent.getStringExtra("RECIPE_TYPE")
        val recetaIngredientes = intent.getStringArrayExtra("RECIPE_INGREDIENTS") ?: arrayOf()
        val recetaIngredientesJson = intent.getStringExtra("RECIPE_INGREDIENTS_JSON")
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)





        if (!recetaId.isNullOrEmpty()) {
            traerRecetaParaEditar(recetaId)
            val tituloPantalla = findViewById<TextView>(R.id.tituloCrear)
            tituloPantalla.text = "Editando Receta"
        } else {
            // Modo crear: agregar uno por defecto
            agregarIngrediente()
            agregarPaso()
        }

        if (!recetaId.isNullOrEmpty()) {
            btnEliminar.visibility = View.VISIBLE

            btnEliminar.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("¿Eliminar receta?")
                    .setMessage("¿Estás segura que querés eliminar esta receta? Esta acción no se puede deshacer.")
                    .setPositiveButton("SÍ") { _, _ ->
                        eliminarReceta(recetaId)
                    }
                    .setNegativeButton("NO", null)
                    .show()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNavigation(R.id.nav_crear)




        tvTipoSeleccionado.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Seleccionar tipo de receta")
            builder.setItems(categorias.toTypedArray()) { _, which ->
                tvTipoSeleccionado.text = categorias[which]
                tvTipoSeleccionado.setTypeface(null, Typeface.BOLD)
            }
            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

        btnElegirFotos.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        tvPorciones.text = porciones.toString()
        btnIncrementar.setOnClickListener {
            if (porciones < 30) porciones++
            tvPorciones.text = porciones.toString()
        }
        btnDecrementar.setOnClickListener {
            if (porciones > 1) porciones--
            tvPorciones.text = porciones.toString()
        }



        findViewById<Button>(R.id.btnPublicar).setOnClickListener {
            if (validarFormulario()) {
                val json = construirJsonReceta()
                if (recetaId.isNullOrEmpty()) {
                    verificarSiExisteRecetaConEseNombre(json)
                } else {
                    val nombreNuevo = json.optString("name", "").trim()
                    if (!nombreNuevo.equals(recetaNombreOriginal, ignoreCase = true)) {
                        verificarSiExisteRecetaConEseNombre(json, recetaId)
                    } else {
                        actualizarReceta(recetaId, json)
                    }
                }


            }
        }



        findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("¿Cancelar receta?")
                .setMessage("Se perderá toda la información ingresada. ¿Estás seguro que querés cancelar?")
                .setPositiveButton("Sí") { _, _ ->
                    val intent = Intent(this, com.example.desarrollotpo.presentation.home.InicioActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }

    }

    private fun verificarSiExisteRecetaConEseNombre(json: JSONObject, idActual: String? = null) {
        val token = TokenUtils.obtenerToken(this)
        val nombre = json.optString("name", "").trim()
        if (nombre.isEmpty()) return

        val client = OkHttpClient()
        val url = "https://desarrolloitpoapi.onrender.com/api/recipes/check-name?name=$nombre"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CrearActivity, "Error verificando receta existente", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val respuesta = JSONObject(response.body?.string() ?: "{}")
                val existe = respuesta.optBoolean("exists", false)
                val idExistente = respuesta.optString("id", "")

                runOnUiThread {
                    if (existe && idExistente.isNotEmpty() && idExistente != idActual) {
                        AlertDialog.Builder(this@CrearActivity)
                            .setTitle("Receta ya existente")
                            .setMessage("Ya tenés una receta con ese nombre. ¿Querés reemplazarla?")
                            .setPositiveButton("Sí, reemplazar") { _, _ ->
                                if (idActual != null) {
                                    // Estás editando → borrar la que estás editando y actualizar la que ya existe
                                    eliminarReceta(idActual) {
                                        actualizarReceta(idExistente, json)
                                    }
                                } else {
                                    // Estás creando → borrar la existente y crear nueva
                                    eliminarReceta(idExistente) {
                                        enviarReceta(json)
                                    }
                                }

                            }
                            .setNegativeButton("No, cancelar", null)
                            .show()
                    } else {
                        if (idActual != null) {
                            actualizarReceta(idActual, json)
                        } else {
                            enviarReceta(json)
                        }
                    }
                }
            }
        })
    }



    private fun construirJsonReceta(): JSONObject {
        val name = findViewById<EditText>(R.id.etNombre).text.toString().trim()
        val description = findViewById<EditText>(R.id.etDescripcion).text.toString().trim()
        val classification = tvTipoSeleccionado.text.toString()
        val portions = porciones

        val ingredientesArray = JSONArray()
        for (i in 0 until contenedorIngredientes.childCount) {
            val view = contenedorIngredientes.getChildAt(i)
            val nombre = view.findViewById<EditText>(R.id.etIngrediente).text.toString().trim()
            val cantidad = view.findViewById<EditText>(R.id.etCantidad).text.toString().toDoubleOrNull() ?: 0.0
            val unidad = view.findViewById<TextView>(R.id.etUnidad).text.toString().trim()

            val ingredienteJson = JSONObject()
            ingredienteJson.put("name", nombre)
            ingredienteJson.put("amount", cantidad)
            ingredienteJson.put("unit", unidad)
            ingredientesArray.put(ingredienteJson)
        }

        val portadaArray = JSONArray()
        for (base64 in imagenesBase64) {
            val foto = JSONObject()
            foto.put("data", base64)
            foto.put("contentType", "image/jpeg")
            portadaArray.put(foto)
        }

        val pasosArray = JSONArray()
        for (i in 0 until contenedorPasos.childCount) {
            val view = contenedorPasos.getChildAt(i)
            val descripcionPaso = view.findViewById<EditText>(R.id.etDescripcionPaso).text.toString().trim()

            val fotosPasoArray = JSONArray()
            for (base64 in fotosPasosBase64.getOrNull(i) ?: emptyList()) {
                val foto = JSONObject()
                foto.put("data", base64)
                foto.put("contentType", "image/jpeg")
                fotosPasoArray.put(foto)
            }

            val pasoJson = JSONObject()
            pasoJson.put("description", descripcionPaso)
            pasoJson.put("photos", fotosPasoArray)
            pasosArray.put(pasoJson)
        }

        val json = JSONObject()
        json.put("name", name)
        json.put("description", description)
        json.put("classification", classification)
        json.put("portions", portions)
        json.put("ingredients", ingredientesArray)
        json.put("frontpagePhotos", portadaArray)
        json.put("steps", pasosArray)

        return json
    }

    private fun validarFormulario(): Boolean {
        val errores = mutableListOf<String>()
        var valido = true

        val etTitulo = findViewById<EditText>(R.id.etNombre)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val errorTextView = findViewById<TextView>(R.id.tvErroresFormulario)

        // Validar título
        val titulo = etTitulo.text.toString().trim()
        if (titulo.isEmpty()) {
            etTitulo.error = "El título es obligatorio"
            errores.add("• El título es obligatorio.")
            valido = false
        } else if (titulo.length > 50) {
            etTitulo.error = "Máximo 50 caracteres"
            errores.add("• El título no debe superar los 50 caracteres.")
            valido = false
        }

        // Validar descripción
        val descripcion = etDescripcion.text.toString().trim()
        if (descripcion.isEmpty()) {
            etDescripcion.error = "La descripción es obligatoria"
            errores.add("• La descripción es obligatoria.")
            valido = false
        } else if (descripcion.length > 100) {
            etDescripcion.error = "Máximo 100 caracteres"
            errores.add("• La descripción no debe superar los 100 caracteres.")
            valido = false
        }

        // Validar categoría (tipo)
        if (tvTipoSeleccionado.text.isNullOrBlank() || tvTipoSeleccionado.text == "Seleccionar" || !categorias.contains(tvTipoSeleccionado.text.toString())) {
            tvTipoSeleccionado.setBackgroundResource(R.drawable.borde_rojo) // Asegurate de tener este drawable
            errores.add("• Seleccioná una categoría de receta.")
            valido = false
        } else {
            tvTipoSeleccionado.background = null
        }

        // Validar fotos de portada
        if (imagenesBase64.isEmpty()) {
            errores.add("• Seleccioná al menos una foto de portada.")
            iconoErrorFoto.visibility = View.VISIBLE
            valido = false
        } else {
            iconoErrorFoto.visibility = View.GONE
        }

        // Validar ingredientes
        if (contenedorIngredientes.childCount == 0) {
            errores.add("• Agregá al menos un ingrediente.")
            valido = false
        } else {
            for (i in 0 until contenedorIngredientes.childCount) {
                val item = contenedorIngredientes.getChildAt(i)
                val etNombre = item.findViewById<EditText>(R.id.etIngrediente)
                val etCantidad = item.findViewById<EditText>(R.id.etCantidad)
                val unidad = item.findViewById<TextView>(R.id.etUnidad)

                if (etNombre.text.isNullOrBlank()) {
                    etNombre.error = "Requerido"
                    errores.add("• Completá el nombre del ingrediente ${i + 1}.")
                    valido = false
                }
                val cantidad = etCantidad.text.toString().toDoubleOrNull() ?: 0.0

                if (cantidad <= 0) {
                    etCantidad.error = "Debe ser mayor a 0"
                    errores.add("• La cantidad del ingrediente ${i + 1} debe ser mayor a 0.")
                    valido = false
                }
                if (unidad.text.isNullOrBlank() || unidad.text == "Seleccionar") {
                    unidad.setBackgroundResource(R.drawable.borde_rojo)
                    errores.add("• Seleccioná una unidad para el ingrediente ${i + 1}.")
                    valido = false
                } else {
                    unidad.background = null
                }
            }
        }

        // Validar pasos
        if (contenedorPasos.childCount == 0) {
            errores.add("• Agregá al menos un paso.")
            valido = false
        } else {
            for (i in 0 until contenedorPasos.childCount) {
                val pasoView = contenedorPasos.getChildAt(i)
                val etDescripcionPaso = pasoView.findViewById<EditText>(R.id.etDescripcionPaso)

                if (etDescripcionPaso.text.isNullOrBlank()) {
                    etDescripcionPaso.error = "Este paso necesita una descripción"
                    errores.add("• El paso ${i + 1} necesita una descripción.")
                    valido = false
                }
            }
        }

        if (!valido) {
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = errores.joinToString("\n")
        } else {
            errorTextView.visibility = View.GONE
            errorTextView.text = ""
        }

        return valido
    }

    private fun agregarIngrediente() {
        if (contenedorIngredientes.childCount >= 10) {
            Toast.makeText(this, "Solo se pueden agregar hasta 10 ingredientes", Toast.LENGTH_SHORT).show()
            return
        }

        val inflater = LayoutInflater.from(this)
        val ingredienteView = inflater.inflate(R.layout.item_ingrediente_crear, contenedorIngredientes, false)

        val etUnidad = ingredienteView.findViewById<TextView>(R.id.etUnidad)
        val btnAgregar = ingredienteView.findViewById<ImageButton>(R.id.btnAgregarIngrediente)
        val btnEliminar = ingredienteView.findViewById<ImageButton>(R.id.btnEliminarIngrediente)

        etUnidad.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Seleccionar unidad")
            builder.setItems(medidas.toTypedArray()) { _, which ->
                etUnidad.text = medidas[which]
                etUnidad.setTypeface(null, Typeface.BOLD)
            }
            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

        btnAgregar.setOnClickListener { agregarIngrediente() }
        btnEliminar.setOnClickListener {
            contenedorIngredientes.removeView(ingredienteView)
            actualizarVisibilidadBotonesIngredientes()
        }

        contenedorIngredientes.addView(ingredienteView)
        actualizarVisibilidadBotonesIngredientes()
    }

    private fun actualizarVisibilidadBotonesIngredientes() {
        val total = contenedorIngredientes.childCount

        for (i in 0 until total) {
            val item = contenedorIngredientes.getChildAt(i)
            val btnAgregar = item.findViewById<ImageButton>(R.id.btnAgregarIngrediente)
            val btnEliminar = item.findViewById<ImageButton>(R.id.btnEliminarIngrediente)

            val mostrarAgregar = (i == total - 1) && (total < 10)
            val mostrarEliminar = (total > 1 && i == total - 1)

            btnAgregar.visibility = if (mostrarAgregar) View.VISIBLE else View.GONE
            btnEliminar.visibility = if (mostrarEliminar) View.VISIBLE else View.GONE
        }
    }

    private fun agregarPaso() {
        if (contenedorPasos.childCount >= 10) {
            Toast.makeText(this, "Solo se pueden agregar hasta 10 pasos", Toast.LENGTH_SHORT).show()
            return
        }

        val inflater = LayoutInflater.from(this)
        val pasoView = inflater.inflate(R.layout.item_paso_receta, contenedorPasos, false)

        fotosPasosBase64.add(mutableListOf()) // 💥 Asegura lista para este paso

        val btnAgregar = pasoView.findViewById<ImageButton>(R.id.btnAgregarPaso)
        val btnEliminar = pasoView.findViewById<ImageButton>(R.id.btnEliminarPaso)
        val btnAgregarImagen = pasoView.findViewById<Button>(R.id.btnAgregarImagenPaso)

        val tvAgregarPaso = pasoView.findViewById<TextView>(R.id.tvAgregarPaso)
        val tvEliminarPaso = pasoView.findViewById<TextView>(R.id.tvEliminarPaso)

        btnAgregar.setOnClickListener { agregarPaso() }
        btnEliminar.setOnClickListener {
            val index = contenedorPasos.indexOfChild(pasoView)
            fotosPasosBase64.removeAt(index)
            contenedorPasos.removeView(pasoView)
            actualizarIndicesPasos()
        }
        btnAgregarImagen.setOnClickListener {
            pasoViewSeleccionadoParaImagen = pasoView
            launcherImagenesPaso.launch("image/*")
        }

        contenedorPasos.addView(pasoView)
        actualizarIndicesPasos()
    }

    private fun actualizarIndicesPasos() {
        val total = contenedorPasos.childCount

        for (i in 0 until total) {
            val pasoView = contenedorPasos.getChildAt(i)
            val tvPaso = pasoView.findViewById<TextView>(R.id.tvPasoNumero)
            val btnAgregar = pasoView.findViewById<ImageButton>(R.id.btnAgregarPaso)
            val btnEliminar = pasoView.findViewById<ImageButton>(R.id.btnEliminarPaso)
            val tvAgregarPaso = pasoView.findViewById<TextView>(R.id.tvAgregarPaso)
            val tvEliminarPaso = pasoView.findViewById<TextView>(R.id.tvEliminarPaso)

            // Numeración
            tvPaso.text = "Paso ${i + 1}"

            // Botón Agregar Paso: solo se muestra en el último, y si hay menos de 10
            val mostrarAgregar = (i == total - 1) && (total < 10)
            btnAgregar.visibility = if (mostrarAgregar) View.VISIBLE else View.GONE
            tvAgregarPaso.visibility = if (mostrarAgregar) View.VISIBLE else View.GONE

            // Botón Eliminar Paso: solo se muestra si no es el primer paso y es el último
            val mostrarEliminar = (i > 0 && i == total - 1)
            btnEliminar.visibility = if (mostrarEliminar) View.VISIBLE else View.GONE
            tvEliminarPaso.visibility = if (mostrarEliminar) View.VISIBLE else View.GONE
        }
    }

    private fun enviarReceta(json: JSONObject) {
        val token = TokenUtils.obtenerToken(this)
        if (token.isEmpty()) {
            Toast.makeText(this, "No se encontró un token. Iniciá sesión.", Toast.LENGTH_LONG).show()
            return
        }

        mostrarLoader() // 👈 Mostrar loader

        val bearer = "Bearer $token"
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = RequestBody.create(mediaType, json.toString())

        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/recipes/create")
            .addHeader("Authorization", bearer)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    ocultarLoader() // 👈 Ocultar loader en error
                    Toast.makeText(this@CrearActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    ocultarLoader() // 👈 Ocultar loader al responder
                    if (response.isSuccessful) {
                        mostrarDialogoConfirmacion()
                    } else {
                        Toast.makeText(this@CrearActivity, "Error ${response.code}: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun eliminarReceta(id: String, callback: (() -> Unit)? = null) {
        val token = TokenUtils.obtenerToken(this)
        if (token.isEmpty()) {
            Toast.makeText(this, "No se encontró un token. Iniciá sesión.", Toast.LENGTH_LONG).show()
            return
        }

        mostrarLoader()

        val bearer = "Bearer $token"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/recipes/$id")
            .addHeader("Authorization", bearer)
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    ocultarLoader()
                    Toast.makeText(this@CrearActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    ocultarLoader()
                    if (response.isSuccessful) {
                        Toast.makeText(this@CrearActivity, "Receta eliminada", Toast.LENGTH_LONG).show()
                        if (callback != null) {
                            callback()
                        } else {
                            finish()
                        }
                    } else {
                        Toast.makeText(this@CrearActivity, "Error ${response.code}: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }


    private fun traerRecetaParaEditar(id: String) {

        val token = TokenUtils.obtenerToken(this)
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/recipes/$id")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CrearActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@CrearActivity, "Error ${response.code}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                val json = JSONObject(response.body?.string() ?: "")

                Log.d("DEBUG", "JSON respuesta: $json")
                runOnUiThread {
                    llenarCamposParaEditar(json)
                }
            }
        })
    }

    private fun llenarCamposParaEditar(json: JSONObject) {
        val name = json.optString("name", "")
        val description = json.optString("description", "")
        val classification = json.optString("classification", "")
        val portions = json.optInt("portions", 1)
        val portadaFull = json.optJSONArray("frontpagePhotos")?.optJSONObject(0)?.optString("base64", "")
        val portada = portadaFull?.substringAfter(",")

        Log.d("DEBUG", "Portada base64: ${portada?.take(100)}")

        findViewById<EditText>(R.id.etNombre).setText(name)
        findViewById<EditText>(R.id.etDescripcion).setText(description)
        tvTipoSeleccionado.text = classification
        tvTipoSeleccionado.setTypeface(null, Typeface.BOLD)
        porciones = portions
        tvPorciones.text = porciones.toString()

        if (!portada.isNullOrEmpty()) {
            Log.d("DEBUG", "Portada detectada, largo: ${portada.length}")
            imagenesBase64.clear()
            imagenesBase64.add(portada)

            val bytes = Base64.decode(portada, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                    setMargins(8, 8, 8, 8)
                }
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            contenedorMiniaturas.removeAllViews()
            contenedorMiniaturas.addView(imageView)
            tvCantidadFotos.text = "(${imagenesBase64.size} seleccionada)"
        }

        // Ingredientes
        contenedorIngredientes.removeAllViews()
        val ingredients = json.optJSONArray("ingredients") ?: JSONArray()
        for (i in 0 until ingredients.length()) {
            val ing = ingredients.getJSONObject(i)
            val nombre = ing.optString("name", "")
            val cantidad = ing.optDouble("amount", 0.0)
            val unidad = ing.optString("unit", "")

            agregarIngrediente()
            val view = contenedorIngredientes.getChildAt(contenedorIngredientes.childCount - 1)
            view.findViewById<EditText>(R.id.etIngrediente).setText(nombre)
            view.findViewById<EditText>(R.id.etCantidad).setText(cantidad.toString())
            view.findViewById<TextView>(R.id.etUnidad).text = unidad
        }

        // Pasos
        contenedorPasos.removeAllViews()
        fotosPasosBase64.clear()
        val steps = json.optJSONArray("steps") ?: JSONArray()
        for (i in 0 until steps.length()) {
            val paso = steps.getJSONObject(i)
            val desc = paso.optString("description", "")
            val fotos = paso.optJSONArray("photos") ?: JSONArray()

            agregarPaso()
            val pasoView = contenedorPasos.getChildAt(i)
            pasoView.findViewById<EditText>(R.id.etDescripcionPaso).setText(desc)

            val contenedorImagenes = pasoView.findViewById<LinearLayout>(R.id.contenedorImagenesPaso)
            contenedorImagenes.removeAllViews()

            for (j in 0 until fotos.length()) {
                val foto = fotos.getJSONObject(j)

                // ⚡️ Usa data o base64 según lo que venga
                val base64 = when {
                    foto.has("data") -> foto.optString("data", "")
                    foto.has("base64") -> foto.optString("base64", "")
                    else -> ""
                }

                if (base64.isBlank()) {
                    Log.d("DEBUG", "Paso $i, foto $j está VACÍA → la salto")
                    continue
                }

                val cleanBase64 = base64.substringAfter(",")
                fotosPasosBase64[i].add(cleanBase64)

                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                val imageView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                        setMargins(8, 8, 8, 8)
                    }
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                contenedorImagenes.addView(imageView)
            }
        }
    }

    private fun actualizarReceta(id: String, json: JSONObject) {
        val token = TokenUtils.obtenerToken(this)
        if (token.isEmpty()) {
            Toast.makeText(this, "No se encontró un token. Iniciá sesión.", Toast.LENGTH_LONG).show()
            return
        }

        mostrarLoader()

        val bearer = "Bearer $token"
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = RequestBody.create(mediaType, json.toString())

        val request = Request.Builder()
            .url("https://desarrolloitpoapi.onrender.com/api/recipes/$id")
            .addHeader("Authorization", bearer)
            .addHeader("Content-Type", "application/json")
            .put(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    ocultarLoader()
                    Toast.makeText(this@CrearActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    ocultarLoader()
                    if (response.isSuccessful) {
                        mostrarDialogoConfirmacion()
                    } else {
                        Toast.makeText(this@CrearActivity, "Error ${response.code}: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun ultimoPasoIndex(): Int = contenedorPasos.childCount - 1

    private fun mostrarLoader() {
        loaderOverlay.visibility = View.VISIBLE
    }

    private fun ocultarLoader() {
        loaderOverlay.visibility = View.GONE
    }


    private fun mostrarDialogoConfirmacion() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Receta enviada")
            .setMessage("Tu receta fue enviada. Deberá ser aprobada por los administradores del sitio antes de que pueda ser visualizada por otros usuarios.")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(this, com.example.desarrollotpo.presentation.home.InicioActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val bytes = stream.toByteArray()
        if (bytes.size > 2 * 1024 * 1024) throw IllegalArgumentException("Imagen supera los 2MB")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}