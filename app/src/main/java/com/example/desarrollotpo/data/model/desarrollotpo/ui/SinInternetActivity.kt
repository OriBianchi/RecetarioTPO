package com.example.desarrollotpo.data.model.desarrollotpo.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import com.example.desarrollotpo.R
import com.example.desarrollotpo.ui.login.WelcomeActivity

class SinInternetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sin_internet)


        val RefrescarButton = findViewById<Button>(R.id.RefrescarButton)

        RefrescarButton.setOnClickListener{
            startActivity(Intent(this, WelcomeActivity::class.java))
        }

        }
    }
