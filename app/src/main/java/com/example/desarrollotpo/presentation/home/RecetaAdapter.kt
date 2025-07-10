package com.example.desarrollotpo.presentation.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import com.example.desarrollotpo.presentation.VerReceta.RecetaDetalleActivity
import com.example.desarrollotpo.presentation.crear.CrearActivity
import com.example.desarrollotpo.utils.TokenUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar
import com.example.desarrollotpo.presentation.adminviews.ModerarRecetasActivity

class RecetaAdapter(private val context: Context, private val recetas: List<Receta>, private val esMisRecetas: Boolean = false) :
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
        val recetaFooterDate: TextView = view.findViewById(R.id.recetaFooterDate)
        val tvFooterRating: TextView = view.findViewById(R.id.tvFooterRating)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
    }

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

// Fecha
        holder.recetaFooterDate.text = "📅 ${formatearFecha(receta.uploadDate)}"

// Rating
        holder.tvFooterRating.text = "⭐ %.1f".format(receta.rating)


        holder.itemView.setOnClickListener {
            val intent = Intent(context, RecetaDetalleActivity::class.java)
            intent.putExtra("RECIPE_ID", receta.id)
            intent.putExtra("IS_SAVED", receta.isSaved)
            (context as Activity).startActivityForResult(intent, 100)
        }

        // Estado de la receta (Pendiente si no está aprobada)
        if (!receta.status) {
            holder.recetaEstado.visibility = View.VISIBLE
            holder.recetaEstado.text = "Pendiente"
        } else {
            holder.recetaEstado.visibility = View.GONE
        }
        val rawImage = receta.frontImage.orEmpty()

// Por defecto: ocultar siempre el botón editar
        holder.btnEdit.visibility = View.GONE

// Solo mostrar en Mis Recetas
        if (esMisRecetas) {
            holder.btnEdit.visibility = View.VISIBLE
        }


        if (rawImage.contains("base64,")) {
            try {
                val base64Clean = rawImage.substringAfter("base64,").trim()
                Log.d("RecetaAdapter", "Base64 recortado (primeros 100): ${base64Clean.take(100)}")

                val imageBytes = Base64.decode(base64Clean, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                if (bitmap != null) {
                    holder.recetaImage.setImageBitmap(bitmap)
                } else {
                    Log.e("RecetaAdapter", "Bitmap es null después de decodificar")
                    holder.recetaImage.setImageResource(R.drawable.placeholder)
                }
            } catch (e: Exception) {
                Log.e("RecetaAdapter", "Error decodificando imagen: ${e.message}")
                holder.recetaImage.setImageResource(R.drawable.placeholder)
            }
        } else {
            Log.e("RecetaAdapter", "No contiene base64, string recibido: ${rawImage.take(50)}")
            holder.recetaImage.setImageResource(R.drawable.placeholder)
        }
        holder.btnEdit.setOnClickListener {
            val intent = Intent(context, CrearActivity::class.java)
            intent.putExtra("RECIPE_ID", receta.id)
            context.startActivity(intent)

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
