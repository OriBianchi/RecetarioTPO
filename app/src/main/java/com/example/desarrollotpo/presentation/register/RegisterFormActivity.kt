package com.example.desarrollotpo.presentation.register

import android.os.Bundle
import com.example.desarrollotpo.R
import com.example.desarrollotpo.core.BaseActivity
import com.google.android.material.button.MaterialButton

class RegisterFormActivity : BaseActivity() { // 👈 importante
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_form)

        val backButton = findViewById<MaterialButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }
}
