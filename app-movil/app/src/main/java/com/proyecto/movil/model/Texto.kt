package com.proyecto.movil.model

// T-435 (US-21): lo que se envía y recibe de POST /api/estudiante/niveles/{id}/texto (T-434)
data class EnviarTextoRequest(val texto: String)

data class TextoRecibido(
    val evidenciaId: Long,
    val nivelId: Int,
    val palabras: Int,
    val palabrasMarcadas: List<String>?
)

// Puntaje de escritura del backend docente (US-22, Noemí); solo lo que muestra la app
data class ResultadoEscritura(
    val puntajeEscritura: Int,
    val estrellas: Int,
    val mensaje: String?
)

sealed class ResultadoEnvioTexto {
    // escritura y reto llegan en null si el backend docente no respondió: el texto igual quedó enviado
    data class Exito(
        val recibido: TextoRecibido,
        val escritura: ResultadoEscritura?,
        val reto: ResultadoReto?
    ) : ResultadoEnvioTexto()
    data class Rechazado(val mensaje: String) : ResultadoEnvioTexto()   // 400/403/404: no se puede enviar así
    object SinConexion : ResultadoEnvioTexto()
    object SesionVencida : ResultadoEnvioTexto()                        // 401
}
