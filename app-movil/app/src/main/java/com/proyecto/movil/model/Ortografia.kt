package com.proyecto.movil.model

// T-330 (US-19): lo que se envía y recibe de POST /api/estudiante/ortografia/verificar (T-328)
data class VerificarTextoRequest(val texto: String)

// inicio y fin: posiciones del error dentro del texto enviado
data class ErrorOrtografico(
    val palabra: String,
    val inicio: Int,
    val fin: Int,
    val tipo: String,            // "ortografia", "tilde" o "mayuscula"
    val sugerencia: String?
)

data class RespuestaOrtografia(
    val errores: List<ErrorOrtografico>,
    val sensibilidad: String,
    val ignorarTildes: Boolean
)

sealed class ResultadoOrtografia {
    data class Exito(val errores: List<ErrorOrtografico>) : ResultadoOrtografia()
    object SinConexion : ResultadoOrtografia()
    object SesionVencida : ResultadoOrtografia()
}
