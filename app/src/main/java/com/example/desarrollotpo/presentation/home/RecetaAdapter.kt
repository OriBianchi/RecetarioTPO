package com.example.desarrollotpo.presentation.home

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import com.example.desarrollotpo.utils.TokenUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class RecetaAdapter(private val context: Context, private val recetas: List<Receta>) :
    RecyclerView.Adapter<RecetaAdapter.RecetaViewHolder>() {

    class RecetaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recetaImage: ImageView = view.findViewById(R.id.recetaImage)
        val recetaTitle: TextView = view.findViewById(R.id.recetaTitle)
        val recetaTipo: TextView = view.findViewById(R.id.recetaTipo)
        val recetaGuardar: Button = view.findViewById(R.id.recetaGuardar)
        val recetaIngredientes: TextView = view.findViewById(R.id.recetaIngredientes)
        val recetaDescripcion: TextView = view.findViewById(R.id.recetaDescripcion)
        val recetaFooter: TextView = view.findViewById(R.id.recetaFooter)
        val recetaEstado: TextView = view.findViewById(R.id.recetaEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecetaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receta_card, parent, false)
        return RecetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecetaViewHolder, position: Int) {
        val receta = recetas[position]
        holder.recetaTitle.text = receta.name
        holder.recetaTipo.text = receta.classification
        holder.recetaIngredientes.text = "Ingredientes: ${receta.ingredients.joinToString(", ")}"
        holder.recetaDescripcion.text = receta.description
        holder.recetaFooter.text = "👤 ${receta.author} • ${receta.stepsCount} pasos"

        // Estado de la receta (Pendiente si no está aprobada)
        if (!receta.status) {
            holder.recetaEstado.visibility = View.VISIBLE
            holder.recetaEstado.text = "Pendiente"
        } else {
            holder.recetaEstado.visibility = View.GONE
        }

        // Imagen desde base64
        if (!receta.frontImage.isNullOrBlank()) {
            try {
                val base64 = receta.frontImage.substringAfter("base64,", "")
                val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.recetaImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.recetaImage.setImageResource(R.drawable.placeholder)
                Log.e("RecetaAdapter", "Error decodificando imagen: ${e.message}")
            }
        } else {
            holder.recetaImage.setImageResource(R.drawable.placeholder)
        }

        // Botón Guardar / Guardada
        holder.recetaGuardar.text = if (receta.isSaved) "Guardada" else "Guardar"

        holder.recetaGuardar.setOnClickListener {
            Log.d("DEBUG_RECETA", "Click en receta: ${receta.name} | Guardada: ${receta.isSaved}")

            val client = OkHttpClient()
            val token = TokenUtils.obtenerToken(holder.itemView.context)
            val url = if (receta.isSaved) {
                "https://desarrolloitpoapi.onrender.com/api/user/recipes/unsave"
            } else {
                "https://desarrolloitpoapi.onrender.com/api/user/recipes/save"
            }

            val jsonBody = """{"recipeId": "${receta.id}"}"""
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            // Optimistic UI update
            receta.isSaved = !receta.isSaved
            holder.recetaGuardar.text = if (receta.isSaved) "Guardada" else "Guardar"

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    receta.isSaved = !receta.isSaved
                    (holder.itemView.context as? Activity)?.runOnUiThread {
                        holder.recetaGuardar.text = if (receta.isSaved) "Guardada" else "Guardar"
                        Toast.makeText(holder.itemView.context, "Error de red", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        receta.isSaved = !receta.isSaved
                        (holder.itemView.context as? Activity)?.runOnUiThread {
                            holder.recetaGuardar.text = if (receta.isSaved) "Guardada" else "Guardar"
                            Toast.makeText(holder.itemView.context, "Error al guardar", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d("DEBUG_RECETA", "Guardado OK")
                    }
                }
            })
        }
    }

    override fun getItemCount() = recetas.size
}
