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

// T-426: resultado de la lectura (T-425); los números llegan en null mientras procesa o si hubo error
data class ResultadoEvaluacion(
    val evidenciaId: Long,
    val nivelId: Int,
    val estado: String,
    val transcripcion: String?,
    val precision: Int?,
    val correctas: Int?,
    val evaluadas: Int?,
    val estrellas: Int?,
    val puntos: Int?,
    val nivelDesbloqueadoId: Int?,
    val nivelDesbloqueadoNombre: String?,
    val asignacionId: Long?,
    val palabras: List<PalabraLeida>? = null,   // T-428: Gson deja null si el backend no la manda
    val mejorAnterior: MejorIntento? = null      // T-432: cómo le fue antes en este nivel
) {
    // T-432: si superó, igualó o no alcanzó su mejor intento anterior; null en su primer intento
    fun mensajeMejorIntento(): String? {
        val anterior = mejorAnterior ?: return null
        val antes = anterior.precision ?: return null
        val ahora = precision ?: return null
        if (anterior.intentos == 0) return null
        return when {
            ahora > antes -> "¡Superaste tu mejor intento!"
            ahora == antes -> "¡Igualaste tu mejor intento!"
            else -> when (val e = anterior.estrellas ?: 0) {
                0 -> "Esta vez te fue un poco menos bien. ¡Tú puedes!"
                1 -> "Tu mejor intento fue de 1 estrella. ¡Tú puedes!"
                else -> "Tu mejor intento fue de $e estrellas. ¡Tú puedes!"
            }
        }
    }

    // T-428: sin repetir y sin puntuación, para la lista "Palabras por mejorar"
    fun palabrasPorMejorar(): List<String> =
        palabras.orEmpty()
            .filter { it.mejorar }
            .map { limpiarPalabra(it.texto) }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
}

// T-432: intentos anteriores ya calificados en el mismo nivel y el mejor de ellos
data class MejorIntento(val intentos: Int, val precision: Int?, val estrellas: Int?)

// T-428: una palabra del texto como está escrita ("cóndor.") y si el niño debe practicarla
data class PalabraLeida(val texto: String, val mejorar: Boolean)

// "¡Hola," -> "Hola": lo que se muestra en la lista y lo que dice la voz del teléfono
fun limpiarPalabra(texto: String): String = texto.trim { !it.isLetterOrDigit() }
