package com.example.desarrollotpo.data.model.desarrollotpo.ui.ForgotPassword

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.desarrollotpo.R

class NewPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_password)
        val backInicio = findViewById<ImageView>(R.id.backInicio)

        backInicio.setOnClickListener {
            startActivity(Intent(this, CodgoCorreoActivity::class.java))
        }


    }
}