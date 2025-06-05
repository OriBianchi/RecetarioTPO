package com.example.desarrollotpo.ui.register

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.desarrollotpo.R
import com.google.android.material.button.MaterialButton

class RegisterFormActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_form)

        val backButton = findViewById<MaterialButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Cierra esta pantalla y vuelve a la anterior
        }
    }
}
