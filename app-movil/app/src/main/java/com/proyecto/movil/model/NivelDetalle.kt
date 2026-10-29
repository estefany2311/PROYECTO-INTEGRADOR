package com.proyecto.movil.model

/*
 * T-215 (US-07): detalle de un nivel. Coincide con la respuesta de
 * GET /api/estudiante/niveles/{id} del backend estudiante (T-214).
 */
data class NivelDetalle(
    val id: Int,
    val orden: Int,
    val nombre: String,
    val tipoReto: String,          // texto del backend; usar `reto` en el código
    val descripcion: String?,
    val estado: String,            // "desbloqueado" o "completado" (un bloqueado responde 403)
    val puntos: Int = 0
) {
    val reto: TipoReto get() = TipoReto.desde(tipoReto)
    val completado: Boolean get() = EstadoNivel.desde(estado) == EstadoNivel.COMPLETADO
}

// Cuerpo del 403: {"mensaje": "Este nivel todavía está cerrado", "nivelAnterior": "Primeros pasos"}
data class ErrorNivel(val mensaje: String?, val nivelAnterior: String?)

// Lo que puede pasar al pedir el detalle de un nivel
sealed class ResultadoNivel {
    data class Exito(val detalle: NivelDetalle) : ResultadoNivel()
    data class Bloqueado(val nivelAnterior: String?) : ResultadoNivel()   // 403
    object SesionVencida : ResultadoNivel()                                // 401
    object NoEncontrado : ResultadoNivel()                                 // 404
    object ErrorConexion : ResultadoNivel()
}
