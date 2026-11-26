package com.proyecto.movil.model

// Comentario que el profesor dejó en una lectura o un texto del alumno (GET /api/estudiante/mensajes)
data class MensajeDocente(
    val evidenciaId: Long,
    val nivelId: Int,
    val nivelNombre: String?,
    val tipo: String?,          // "audio" = lectura, "texto" = escritura
    val comentario: String,
    val fecha: String?          // "2026-11-26T04:30:00"
)

fun MensajeDocente.nombreReto(): String = when (tipo) {
    "audio" -> "Lectura"
    "texto" -> "Escritura"
    else -> "Reto"
}

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")

// "2026-11-26T04:30:00" -> "26 nov". Sin java.time porque la app llega hasta Android 7 (minSdk 24)
fun fechaCorta(fecha: String?): String {
    val partes = fecha?.take(10)?.split("-") ?: return ""
    if (partes.size != 3) return ""
    val mes = partes[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return ""
    val dia = partes[2].toIntOrNull() ?: return ""
    return "$dia ${MESES[mes - 1]}"
}
