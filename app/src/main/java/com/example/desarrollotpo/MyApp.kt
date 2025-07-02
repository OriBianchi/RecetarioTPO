package com.example.desarrollotpo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Fuerza el modo claro en toda la app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
