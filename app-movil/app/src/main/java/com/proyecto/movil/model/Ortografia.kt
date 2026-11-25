package com.proyecto.movil.model

// T-330 (US-19): lo que se envía y recibe de POST /api/estudiante/ortografia/verificar (T-328)
data class VerificarTextoRequest(val texto: String)

// inicio y fin: posiciones del error dentro del texto enviado
data class ErrorOrtografico(
    val palabra: String,
    val inicio: Int,
    val fin: Int,
    val tipo: String,            // "ortografia", "tilde" o "mayuscula"
    val sugerencia: String?,
    val sugerencias: List<String>? = null   // US-20 (T-364): hasta 3, de la más parecida a la menos
) {
    // T-366: lo que muestra el menú; si el backend no manda la lista, se usa la única sugerencia
    fun opciones(): List<String> =
        sugerencias?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: listOfNotNull(sugerencia)
}

// T-367 (US-20): el texto con la palabra del error cambiada por la sugerencia elegida.
// null si la palabra ya no está en ese lugar (el niño siguió escribiendo)
fun reemplazarPalabra(texto: String, error: ErrorOrtografico, por: String): String? {
    if (error.inicio < 0 || error.fin > texto.length || error.inicio > error.fin) return null
    if (texto.substring(error.inicio, error.fin) != error.palabra) return null
    return texto.substring(0, error.inicio) + por + texto.substring(error.fin)
}

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
