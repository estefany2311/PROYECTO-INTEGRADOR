package com.proyecto.movil.model

/*
 * T-219 (US-10): el texto que el niño lee en un nivel.
 * Coincide con GET /api/estudiante/niveles/{id}/lectura del backend estudiante (T-218).
 */
data class LecturaNivel(
    val nivelId: Int,
    val lecturaId: Long,
    val titulo: String,
    val texto: String,
    val grado: Int,
    val dificultad: String,
    val asignadaPorDocente: Boolean
)

// Lo que puede pasar al pedir la lectura de un nivel
sealed class ResultadoLectura {
    data class Exito(val lectura: LecturaNivel) : ResultadoLectura()
    data class Bloqueado(val nivelAnterior: String?) : ResultadoLectura()   // 403
    object SinLectura : ResultadoLectura()                                 // 404: aún no hay lectura para su grado
    object SesionVencida : ResultadoLectura()                              // 401
    object ErrorConexion : ResultadoLectura()
}
