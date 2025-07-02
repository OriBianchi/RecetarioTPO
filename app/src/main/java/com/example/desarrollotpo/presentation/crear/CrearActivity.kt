package com.example.desarrollotpo.presentation.crear

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.desarrollotpo.R
import com.example.desarrollotpo.utils.setupBottomNavigation

class CrearActivity : AppCompatActivity() {

    private lateinit var tvPorciones: TextView
    private lateinit var btnIncrementar: Button
    private lateinit var btnDecrementar: Button
    private lateinit var dropdownCategoria: AutoCompleteTextView
    private lateinit var contenedorIngredientes: LinearLayout

    private var porciones: Int = 1
    private val medidas = listOf("unidad", "gramos", "tazas", "cucharadas", "cc")

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
        dropdownCategoria = findViewById(R.id.etTipo)
        contenedorIngredientes = findViewById(R.id.contenedorIngredientes)

        // Configurar dropdown de categorías
        val categorias = listOf(
            "Desayuno", "Almuerzo", "Cena", "Merienda", "Snack",
            "Vegano", "Vegetariano", "Sin TACC", "Otro"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
        dropdownCategoria.setAdapter(adapter)

        // Configurar contador de porciones
        tvPorciones.text = porciones.toString()
        btnIncrementar.setOnClickListener {
            if (porciones < 30) {
                porciones++
                tvPorciones.text = porciones.toString()
            }
        }
        btnDecrementar.setOnClickListener {
            if (porciones > 1) {
                porciones--
                tvPorciones.text = porciones.toString()
            }
        }

        // Agregar primer ingrediente (obligatorio)
        agregarIngrediente()
    }

    private fun agregarIngrediente() {
        val inflater = LayoutInflater.from(this)
        val ingredienteView = inflater.inflate(R.layout.item_ingrediente_crear, contenedorIngredientes, false)

        val etIngrediente = ingredienteView.findViewById<EditText>(R.id.etIngrediente)
        val etCantidad = ingredienteView.findViewById<EditText>(R.id.etCantidad)
        val dropdownUnidad = ingredienteView.findViewById<AutoCompleteTextView>(R.id.etUnidad)
        val btnAgregar = ingredienteView.findViewById<ImageButton>(R.id.btnAgregarIngrediente)
        val btnEliminar = ingredienteView.findViewById<ImageButton>(R.id.btnEliminarIngrediente)

        // Configurar spinner de unidad
        val unidadAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, medidas)
        dropdownUnidad.setAdapter(unidadAdapter)

        // Agregar lógica de agregar nuevo ingrediente
        btnAgregar.setOnClickListener {
            agregarIngrediente()
        }

        // Eliminar solo si no es el primer ingrediente
        btnEliminar.setOnClickListener {
            contenedorIngredientes.removeView(ingredienteView)
            actualizarVisibilidadBotones()
        }

        contenedorIngredientes.addView(ingredienteView)
        actualizarVisibilidadBotones()
    }

    private fun actualizarVisibilidadBotones() {
        val total = contenedorIngredientes.childCount
        for (i in 0 until total) {
            val itemView = contenedorIngredientes.getChildAt(i)
            val btnAgregar = itemView.findViewById<ImageButton>(R.id.btnAgregarIngrediente)
            val btnEliminar = itemView.findViewById<ImageButton>(R.id.btnEliminarIngrediente)

            btnAgregar.visibility = if (i == total - 1) View.VISIBLE else View.GONE
            btnEliminar.visibility = if (total > 1 && i == total - 1) View.VISIBLE else View.GONE
        }
    }
}
