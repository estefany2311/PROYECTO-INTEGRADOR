package com.proyecto.movil.model

// T-418 (US-13): si la IA ya escuchó la lectura (procesando, listo o error), según T-416
data class EstadoEvidencia(
    val evidenciaId: Long,
    val estado: String,
    val transcripcion: String?,
    val mensaje: String?
)

sealed class ResultadoEstado {
    data class Exito(val estado: EstadoEvidencia) : ResultadoEstado()
    object NoEncontrada : ResultadoEstado()   // 404
    object SesionVencida : ResultadoEstado()  // 401
    object SinConexion : ResultadoEstado()
}
