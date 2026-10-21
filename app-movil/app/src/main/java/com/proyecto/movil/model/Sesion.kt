package com.proyecto.movil.model

// Lo que se envía a POST api/estudiante/sesion
data class InicioSesionRequest(
    val estudianteId: Long,
    val pin: String
)

// Respuesta 200: el token identifica al estudiante en las siguientes peticiones
data class SesionRespuesta(
    val token: String,
    val estudianteId: Long,
    val nombre: String,
    val avatarUrl: String
)

// Cuerpo de los errores del backend. Según el error vienen unos campos u otros, por eso son opcionales.
data class ErrorApi(
    val mensaje: String?,
    val intentosRestantes: Int?,
    val segundosRestantes: Long?
)

sealed class ResultadoLogin {
    data class Exito(val sesion: SesionRespuesta) : ResultadoLogin()
    data class PinIncorrecto(val intentosRestantes: Int?) : ResultadoLogin()
    data class Bloqueado(val segundosRestantes: Long?) : ResultadoLogin()
    data class ErrorConexion(val mensaje: String) : ResultadoLogin()
}
