package com.example.desarrollotpo.data.model.desarrollotpo.ui.ForgotPassword

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.desarrollotpo.R
import android.widget.Toast

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val emailEditText = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.emailInput)
        val enviarButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.enviarCodigoButton)

        enviarButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresá tu correo", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "El correo no es válido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Validación OK ", Toast.LENGTH_SHORT).show()
            }
        }









    }
}
