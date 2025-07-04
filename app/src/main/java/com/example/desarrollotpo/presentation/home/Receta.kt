package com.example.desarrollotpo.presentation.home

data class Receta(
    val id: String,
    val name: String,
    val classification: String,
    val ingredients: List<String>,
    val description: String,
    val frontImage: String,
    val author: String,
    val stepsCount: Int,
    var isSaved: Boolean,
    val status: Boolean,
    val uploadDate: String,
    val rating: Double,
    val portions: Int,
    val stepsJson: String,
    val ingredientsJson: String
)
