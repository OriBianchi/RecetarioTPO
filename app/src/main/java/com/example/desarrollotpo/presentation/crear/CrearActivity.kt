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


    private var porciones: Int = 1
    private val medidas = listOf("g", "kg", "unidades", "tazas", "ml", "cucharadas", "cucharaditas", "pizca", "litros", "cc")
    private val categorias = listOf("Desayuno", "Almuerzo", "Cena", "Merienda", "Snack", "Vegano", "Vegetariano", "Sin TACC", "Otro")
    private val imagenesBase64 = mutableListOf<String>()
    private val maxImages = 3
    private var pasoViewSeleccionadoParaImagen: View? = null

    private val launcherImagenesPaso = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        pasoViewSeleccionadoParaImagen?.let { pasoView ->
            val contenedorImagenes = pasoView.findViewById<LinearLayout>(R.id.contenedorImagenesPaso)
            val tvCantidad = pasoView.findViewById<TextView>(R.id.tvCantidadImagenesPaso)

            contenedorImagenes.removeAllViews()
            val seleccionadas = uris.take(2)
            tvCantidad.text = "(${seleccionadas.size} seleccionadas)"

            for (uri in seleccionadas) {
                val bitmap = uriToBitmap(uri)
                val compressed = compressBitmap(bitmap, 90)
                val base64 = bitmapToBase64(compressed)

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


    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        imagenesBase64.clear()
        contenedorMiniaturas.removeAllViews()

        uris.take(maxImages).forEach { uri ->
            val bitmap = uriToBitmap(uri)
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

        tvCantidadFotos.text = "(${imagenesBase64.size} seleccionadas)"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_receta)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNavigation(R.id.nav_crear)

        tvPorciones = findViewById(R.id.tvPorciones)
        btnIncrementar = findViewById(R.id.btnIncrementarPorciones)
        btnDecrementar = findViewById(R.id.btnDecrementarPorciones)
        contenedorIngredientes = findViewById(R.id.contenedorIngredientes)
        contenedorPasos = findViewById(R.id.contenedorPasos)
        tvTipoSeleccionado = findViewById(R.id.tvTipoSeleccionado)
        btnElegirFotos = findViewById(R.id.btnElegirFotosPortada)
        tvCantidadFotos = findViewById(R.id.tvCantidadFotos)
        contenedorMiniaturas = findViewById(R.id.contenedorMiniaturas)

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

        agregarIngrediente()
        agregarPaso()

        findViewById<Button>(R.id.btnPublicar).setOnClickListener {
            if (validarFormulario()) {
                val json = construirJsonReceta()
                enviarReceta(json)
            }
        }

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

            val contenedorImagenes = view.findViewById<LinearLayout>(R.id.contenedorImagenesPaso)
            val fotosPasoArray = JSONArray()
            for (j in 0 until contenedorImagenes.childCount) {
                val imageView = contenedorImagenes.getChildAt(j) as ImageView
                imageView.drawable?.let { drawable ->
                    val bitmap = imageView.drawable.toBitmap()
                    val compressed = compressBitmap(bitmap, 90)
                    val base64 = bitmapToBase64(compressed)

                    val foto = JSONObject()
                    foto.put("data", base64)
                    foto.put("contentType", "image/jpeg")
                    fotosPasoArray.put(foto)
                }
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
        var valido = true

        // Validar título
        val etTitulo = findViewById<EditText>(R.id.etNombre)
        val titulo = etTitulo.text.toString().trim()
        if (titulo.isEmpty()) {
            etTitulo.error = "El título es obligatorio"
            valido = false
        } else if (titulo.length > 50) {
            etTitulo.error = "Máximo 50 caracteres"
            valido = false
        }

        // Validar descripción
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val descripcion = etDescripcion.text.toString().trim()
        if (descripcion.isEmpty()) {
            etDescripcion.error = "La descripción es obligatoria"
            valido = false
        } else if (descripcion.length > 100) {
            etDescripcion.error = "Máximo 100 caracteres"
            valido = false
        }

        // Validar tipo
        if (tvTipoSeleccionado.text.isNullOrBlank() || tvTipoSeleccionado.text == "Seleccionar tipo") {
            tvTipoSeleccionado.error = "Seleccioná una categoría"
            valido = false
        } else {
            tvTipoSeleccionado.error = null
        }

        // Validar ingredientes
        if (contenedorIngredientes.childCount == 0) {
            Toast.makeText(this, "Agregá al menos un ingrediente", Toast.LENGTH_SHORT).show()
            valido = false
        } else {
            for (i in 0 until contenedorIngredientes.childCount) {
                val item = contenedorIngredientes.getChildAt(i)
                val etNombre = item.findViewById<EditText>(R.id.etIngrediente)
                val etCantidad = item.findViewById<EditText>(R.id.etCantidad)
                val unidad = item.findViewById<TextView>(R.id.etUnidad)

                if (etNombre.text.isNullOrBlank()) {
                    etNombre.error = "Requerido"
                    valido = false
                }
                if (etCantidad.text.isNullOrBlank()) {
                    etCantidad.error = "Requerido"
                    valido = false
                }
                if (unidad.text.isNullOrBlank() || unidad.text == "Unidad") {
                    unidad.error = "Seleccioná unidad"
                    valido = false
                } else {
                    unidad.error = null
                }
            }
        }

        // Validar pasos
        if (contenedorPasos.childCount == 0) {
            Toast.makeText(this, "Agregá al menos un paso", Toast.LENGTH_SHORT).show()
            valido = false
        } else {
            for (i in 0 until contenedorPasos.childCount) {
                val pasoView = contenedorPasos.getChildAt(i)
                val etDescripcionPaso = pasoView.findViewById<EditText>(R.id.etDescripcionPaso)

                if (etDescripcionPaso.text.isNullOrBlank()) {
                    etDescripcionPaso.error = "Este paso necesita una descripción"
                    valido = false
                }
            }
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

        val btnAgregar = pasoView.findViewById<ImageButton>(R.id.btnAgregarPaso)
        val btnEliminar = pasoView.findViewById<ImageButton>(R.id.btnEliminarPaso)
        val btnAgregarImagen = pasoView.findViewById<Button>(R.id.btnAgregarImagenPaso)
        val tvPaso = pasoView.findViewById<TextView>(R.id.tvPasoNumero)

        val tvAgregarPaso = pasoView.findViewById<TextView>(R.id.tvAgregarPaso)
        val tvEliminarPaso = pasoView.findViewById<TextView>(R.id.tvEliminarPaso)

        btnAgregar.setOnClickListener { agregarPaso() }
        btnEliminar.setOnClickListener {
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
                    Toast.makeText(this@CrearActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CrearActivity, "Receta enviada con éxito", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@CrearActivity, "Error ${response.code}: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun ultimoPasoIndex(): Int = contenedorPasos.childCount - 1

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
        if (bytes.size > 3 * 1024 * 1024) throw IllegalArgumentException("Imagen supera los 3MB")
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
