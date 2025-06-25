package com.example.desarrollotpo.presentation.misrecetas

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.desarrollotpo.R
import com.example.desarrollotpo.utils.setupBottomNavigation


class MisRecetasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_mis_recetas)
        setupBottomNavigation(R.id.nav_mis_recetas)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}