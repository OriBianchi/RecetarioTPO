package com.example.desarrollotpo.presentation.home

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import com.example.desarrollotpo.utils.TokenUtils
import com.squareup.picasso.Picasso
import okhttp3.*
import java.io.IOException
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

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

        // Imagen
        if (receta.frontImage.isNotBlank()) {
            Picasso.get()
                .load(receta.frontImage)
                .into(holder.recetaImage)
        } else {
            holder.recetaImage.setImageResource(R.drawable.placeholder)
        }

        // Texto del botón
        holder.recetaGuardar.text = if (receta.isSaved) "Guardada" else "Guardar"

        // Lógica para guardar o desguardar
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

            // ✅ 1) Marcar cambio al toque (optimistic update)
            receta.isSaved = !receta.isSaved
            holder.recetaGuardar.text = if (receta.isSaved) "Guardada" else "Guardar"

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("DEBUG_RECETA", "Fallo red: ${e.message}")
                    // ✅ 2) Si falla, revertir y avisar
                    receta.isSaved = !receta.isSaved
                    (holder.itemView.context as? Activity)?.runOnUiThread {
                        holder.recetaGuardar.text = if (receta.isSaved) "Guardada" else "Guardar"
                        Toast.makeText(holder.itemView.context, "Error de red", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("DEBUG_RECETA", "Código respuesta: ${response.code}")
                    Log.d("DEBUG_RECETA", "Body respuesta: ${response.body?.string()}")
                    if (!response.isSuccessful) {
                        // ✅ 3) Si el server rechaza, revertir y avisar
                        receta.isSaved = !receta.isSaved
                        (holder.itemView.context as? Activity)?.runOnUiThread {
                            holder.recetaGuardar.text = if (receta.isSaved) "Guardada" else "Guardar"
                            Toast.makeText(holder.itemView.context, "Error al guardar", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // ✅ 4) Todo OK, opcional: mostrar éxito (si querés)
                        Log.d("DEBUG_RECETA", "Guardado OK")
                    }
                }
            })
        }
    }

    override fun getItemCount() = recetas.size
}
