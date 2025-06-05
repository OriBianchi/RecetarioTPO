// WelcomeActivity implementation goes here
package com.example.desarrollotpo.data.model.desarrollotpo.presentation.common

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.desarrollotpo.R
import com.example.desarrollotpo.ui.login.LoginFormActivity
import com.example.desarrollotpo.ui.register.RegisterFormActivity
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val registerButton = findViewById<MaterialButton>(R.id.registerButton)

        loginButton.setOnClickListener {
            // Ir a formulario de login real
            startActivity(Intent(this, LoginFormActivity::class.java)) // si lo creás
        }

        registerButton.setOnClickListener {
            // Mostrar info o ir a pantalla de registro
            startActivity(Intent(this, RegisterFormActivity::class.java))
        }
    }
}
