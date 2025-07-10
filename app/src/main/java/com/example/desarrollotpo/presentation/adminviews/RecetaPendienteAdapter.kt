import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import com.example.desarrollotpo.presentation.VerReceta.RecetaDetalleActivity
import com.example.desarrollotpo.presentation.home.Receta
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import android.util.Base64
import android.widget.ImageButton

class RecetaPendienteAdapter(
    private val context: Context,
    private val recetas: MutableList<Receta>,
    private val onAprobada: (Receta) -> Unit,
    private val onRechazada: (Receta) -> Unit
) : RecyclerView.Adapter<RecetaPendienteAdapter.RecetaViewHolder>() {

    inner class RecetaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recetaImage: ImageView = view.findViewById(R.id.recetaImage)
        val recetaTitle: TextView = view.findViewById(R.id.recetaTitle)
        val recetaTipo: TextView = view.findViewById(R.id.recetaTipo)
        val recetaIngredientes: TextView = view.findViewById(R.id.recetaIngredientes)
        val recetaDescripcion: TextView = view.findViewById(R.id.recetaDescripcion)
        val recetaFooter: TextView = view.findViewById(R.id.recetaFooter)
        val recetaFooterDate: TextView = view.findViewById(R.id.recetaFooterDate)
        val tvFooterRating: TextView = view.findViewById(R.id.tvFooterRating)
        val recetaEstado: TextView = view.findViewById(R.id.recetaEstado)
        val btnAprobar: Button = view.findViewById(R.id.btnAprobar)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazar)
        val recetaGuardar: Button = view.findViewById(R.id.recetaGuardar)
        val botonesModeracion: View = view.findViewById(R.id.botonesModeracion)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecetaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receta_card, parent, false)
        return RecetaViewHolder(view)
    }

    override fun getItemCount(): Int = recetas.size

    override fun onBindViewHolder(holder: RecetaViewHolder, position: Int) {
        val receta = recetas[position]

        holder.recetaTitle.text = receta.name
        holder.recetaTipo.text = receta.classification
        holder.recetaIngredientes.text = "Ingredientes: ${receta.ingredients.joinToString(", ")}"
        holder.recetaDescripcion.text = receta.description
        holder.recetaFooter.text = "👤 ${receta.author} • ${receta.stepsCount} pasos"
        holder.recetaFooterDate.text = "📅 ${formatearFecha(receta.uploadDate)}"
        holder.tvFooterRating.text = "⭐ %.1f".format(receta.rating)

        holder.recetaEstado.visibility = View.VISIBLE
        holder.recetaEstado.text = "Pendiente"

        val rawImage = receta.frontImage.orEmpty()
        if (rawImage.contains("base64,")) {
            val base64Clean = rawImage.substringAfter("base64,").trim()
            val imageBytes = Base64.decode(base64Clean, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            holder.recetaImage.setImageBitmap(bitmap)
        } else {
            holder.recetaImage.setImageResource(R.drawable.placeholder)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, RecetaDetalleActivity::class.java)
            intent.putExtra("RECIPE_ID", receta.id)
            (context as Activity).startActivityForResult(intent, 100)
        }

        holder.btnAprobar.setOnClickListener {
            onAprobada(receta)
        }

        holder.btnRechazar.setOnClickListener {
            onRechazada(receta)
        }

        // Ocultar botón Guardar y Editar (no aplican en moderación)
        holder.recetaGuardar.visibility = View.GONE
        holder.btnEdit.visibility = View.GONE

        // Mostrar botones de Aprobar/Rechazar
        holder.botonesModeracion.visibility = View.VISIBLE

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
}
