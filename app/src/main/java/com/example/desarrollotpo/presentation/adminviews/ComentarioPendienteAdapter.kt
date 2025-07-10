import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.desarrollotpo.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ComentarioPendienteAdapter(
    private val context: Context,
    private val comentarios: List<ComentarioPendiente>,
    private val onAprobar: (ComentarioPendiente) -> Unit,
    private val onRechazar: (ComentarioPendiente) -> Unit
) : RecyclerView.Adapter<ComentarioPendienteAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo = view.findViewById<TextView>(R.id.tvRecipeTitle)
        val tvUserDate = view.findViewById<TextView>(R.id.tvUserDate)
        val tvComentario = view.findViewById<TextView>(R.id.tvComentario)
        val btnAprobar = view.findViewById<Button>(R.id.btnAprobar)
        val btnRechazar = view.findViewById<Button>(R.id.btnRechazar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(context)
            .inflate(R.layout.item_comentario_pendiente, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = comentarios.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = comentarios[position]

        holder.tvTitulo.text = item.recipeName
        holder.tvUserDate.text = "By ${item.username} - ${formatFecha(item.createdAt)}"
        holder.tvComentario.text = item.text

        holder.btnAprobar.setOnClickListener { onAprobar(item) }
        holder.btnRechazar.setOnClickListener { onRechazar(item) }
    }

    private fun formatFecha(fechaISO: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(fechaISO)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "Fecha inválida"
        }
    }
}
