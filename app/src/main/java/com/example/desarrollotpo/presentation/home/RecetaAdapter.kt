package com.example.desarrollotpo.presentation.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import com.squareup.picasso.Picasso

class RecetaAdapter(private val recetas: List<Receta>) :
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

        // Cargar imagen
        if (receta.frontImage.isNotBlank()) {
            Picasso.get()
                .load(receta.frontImage)
                .into(holder.recetaImage)
        } else {
            holder.recetaImage.setImageResource(R.drawable.placeholder) // Usá un ícono local
        }

    }

    override fun getItemCount() = recetas.size
}
