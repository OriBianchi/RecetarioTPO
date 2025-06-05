package com.example.desarrollotpo.data.model.desarrollotpo.presentation.home

data class Receta(
    val name: String,
    val classification: String,
    val ingredients: List<String>,
    val description: String,
    val frontImage: String,
    val author: String,
    val stepsCount: Int
)
