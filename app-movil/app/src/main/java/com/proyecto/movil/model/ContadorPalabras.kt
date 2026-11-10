package com.proyecto.movil.model

/*
 * T-324 (US-18): cuenta las palabras que escribió el niño.
 * Una palabra es un grupo seguido de letras o números (incluye tildes, ñ y ü):
 *  - los signos no cuentan: "¡Hola!" es 1 palabra y "..." no es ninguna
 *  - varios espacios o saltos de línea seguidos no suman palabras
 *  - "perro-gato" son 2 palabras
 */
private val PALABRA = Regex("[\\p{L}\\p{N}]+")

fun contarPalabras(texto: String): Int = PALABRA.findAll(texto).count()
