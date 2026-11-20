package com.proyecto.movil.model

// T-417 (US-13): lo que responde el backend al subir la grabación
data class AudioRecibido(
    val evidenciaId: Long,
    val nivelId: Int,
    val lecturaId: Long,
    val duracionMs: Long,
    val estado: String
)

sealed class ResultadoEnvio {
    data class Exito(val recibido: AudioRecibido) : ResultadoEnvio()
    data class Rechazado(val mensaje: String) : ResultadoEnvio()   // 400/403/404/413/415: hay que volver a grabar
    object SinConexion : ResultadoEnvio()                          // sin internet o el servidor falló: se puede reintentar
    object SesionVencida : ResultadoEnvio()                        // 401
}
