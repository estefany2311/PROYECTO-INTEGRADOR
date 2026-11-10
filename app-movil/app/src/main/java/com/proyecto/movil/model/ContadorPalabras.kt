package com.proyecto.movil.model

//T-324 (US-18): cuenta las palabras que escribió el niño.

private val PALABRA = Regex("[\\p{L}\\p{N}]+")

fun contarPalabras(texto: String): Int = PALABRA.findAll(texto).count()
