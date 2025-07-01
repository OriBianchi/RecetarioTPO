package com.example.desarrollotpo.presentation.home

data class Receta(
    val id: String,
    val name: String,
    val classification: String,
    val ingredients: List<String>,
    val description: String,
    val frontImage: String?,
    val author: String,
    val stepsCount: Int,
    var isSaved: Boolean,
    val status: Boolean
)
