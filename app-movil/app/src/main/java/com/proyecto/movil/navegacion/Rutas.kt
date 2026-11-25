package com.proyecto.movil.navegacion

import android.net.Uri

// Todas las pantallas de la app en un solo lugar: el patrón de cada ruta y cómo armarla con sus datos
object Rutas {

    const val ARG_CODIGO_AULA = "codigoAula"
    const val ARG_ESTUDIANTE = "estudianteId"
    const val ARG_NOMBRE = "nombre"
    const val ARG_AVATAR = "avatar"
    const val ARG_NIVEL = "nivelId"
    const val ARG_EVIDENCIA = "evidenciaId"

    const val CODIGO_AULA = "codigoAula"
    const val PERFILES = "perfiles/{$ARG_CODIGO_AULA}"
    const val PIN = "pin/{$ARG_ESTUDIANTE}?$ARG_NOMBRE={$ARG_NOMBRE}&$ARG_AVATAR={$ARG_AVATAR}"
    const val MAPA = "mapa"
    const val NIVEL = "nivel/{$ARG_NIVEL}"
    const val LECTURA = "lectura/{$ARG_NIVEL}"
    const val GRABAR = "grabar/{$ARG_NIVEL}"
    const val CONSIGNA = "consigna/{$ARG_NIVEL}"
    const val ESCRIBIR = "escribir/{$ARG_NIVEL}"
    const val ESPERA = "espera/{$ARG_EVIDENCIA}"

    // Uri.encode: los nombres pueden tener tildes o espacios y el avatar es una URL con ? y &
    fun perfiles(codigoAula: String) = "perfiles/${Uri.encode(codigoAula)}"
    fun pin(estudianteId: Long, nombre: String, avatarUrl: String) =
        "pin/$estudianteId?$ARG_NOMBRE=${Uri.encode(nombre)}&$ARG_AVATAR=${Uri.encode(avatarUrl)}"
    fun nivel(nivelId: Int) = "nivel/$nivelId"
    fun lectura(nivelId: Int) = "lectura/$nivelId"
    fun grabar(nivelId: Int) = "grabar/$nivelId"
    fun consigna(nivelId: Int) = "consigna/$nivelId"
    fun escribir(nivelId: Int) = "escribir/$nivelId"
    fun espera(evidenciaId: Long) = "espera/$evidenciaId"
}
