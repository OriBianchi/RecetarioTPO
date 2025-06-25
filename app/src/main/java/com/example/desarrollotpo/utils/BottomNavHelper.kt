package com.example.desarrollotpo.utils

import android.app.Activity
import android.content.Intent
import com.example.desarrollotpo.R
import com.example.desarrollotpo.presentation.crear.CrearActivity
import com.example.desarrollotpo.presentation.guardados.GuardadosActivity
import com.example.desarrollotpo.presentation.home.InicioActivity
import com.example.desarrollotpo.presentation.misrecetas.MisRecetasActivity
import com.example.desarrollotpo.presentation.perfil.PerfilActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

fun Activity.setupBottomNavigation(currentItemId: Int) {
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
    bottomNav.selectedItemId = currentItemId

    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_inicio -> {
                if (this !is InicioActivity) {
                    startActivity(Intent(this, InicioActivity::class.java))
                    finish()
                }
                true
            }
            R.id.nav_Guardados -> {
                if (this !is GuardadosActivity) {
                    startActivity(Intent(this, GuardadosActivity::class.java))
                    finish()
                }
                true
            }
            R.id.nav_crear -> {
                if (this !is CrearActivity) {
                    startActivity(Intent(this, CrearActivity::class.java))
                    finish()
                }
                true
            }
            R.id.nav_mis_recetas -> {
                if (this !is MisRecetasActivity) {
                    startActivity(Intent(this, MisRecetasActivity::class.java))
                    finish()
                }
                true
            }
            R.id.nav_perfil -> {
                if (this !is PerfilActivity) {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    finish()
                }
                true
            }
            else -> false
        }
    }
}
