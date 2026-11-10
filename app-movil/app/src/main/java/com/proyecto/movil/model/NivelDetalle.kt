package com.proyecto.movil.model

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

// T-216: lo que la tablet envía al terminar un reto (POST /api/estudiante/niveles/{id}/completar, T-198/T-309)
data class CompletarNivelRequest(val puntos: Int, val estrellas: Int)

// T-216: respuesta del backend. aprobado = sacó las estrellas mínimas y se desbloqueó el siguiente
data class ResultadoReto(
    val nivelId: Int,
    val estado: String,
    val puntos: Int,
    val estrellas: Int,
    val aprobado: Boolean,
    val estrellasMinimas: Int,
    val siguienteNivelId: Int?,
    val siguienteNivelNombre: String?,
    val mejoresPuntos: Int = 0,                      // T-310: el mejor puntaje guardado
    val siguienteRecienDesbloqueado: Boolean = false // T-310: true solo la primera vez
)
