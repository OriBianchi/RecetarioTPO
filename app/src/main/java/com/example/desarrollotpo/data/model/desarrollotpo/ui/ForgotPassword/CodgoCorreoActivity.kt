package com.example.desarrollotpo.data.model.desarrollotpo.ui.ForgotPassword

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import com.example.desarrollotpo.R

class CodgoCorreoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_codgo_correo)
        val backInicio = findViewById<ImageView>(R.id.backInicio)

        backInicio.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        }
    }
