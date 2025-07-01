package com.example.desarrollotpo.presentation.perfil

data class UserResponse(
    val username: String,
    val role: String,
    val savedRecipes: List<String>
)