package com.proyecto.movil.model

/*
 * T-209 (US-05): modelo de niveles y progreso del mapa de aventura.
 * Coincide con la respuesta de GET /api/estudiante/mapa (backend docente, T-197 de Noemí).
 * Las tablas nivel y progreso, y los 6 niveles semilla, los crea la migración V7 del backend docente.
 */

// Tipos de reto de cada nivel. El backend los manda en minúsculas: "lectura", "escritura", "oralidad"
enum class TipoReto {
    LECTURA, ESCRITURA, ORALIDAD, DESCONOCIDO;

    companion object {
        fun desde(valor: String?): TipoReto =
            entries.firstOrNull { it.name.equals(valor, ignoreCase = true) } ?: DESCONOCIDO
    }
}

// Estado de un nivel para el alumno (US-06): "bloqueado", "desbloqueado", "completado"
enum class EstadoNivel {
    BLOQUEADO, DESBLOQUEADO, COMPLETADO;

    companion object {
        // Si llega un valor desconocido se trata como bloqueado: nunca se abre un nivel por error
        fun desde(valor: String?): EstadoNivel =
            entries.firstOrNull { it.name.equals(valor, ignoreCase = true) } ?: BLOQUEADO
    }
}

// Un nivel del recorrido, tal como lo manda el backend
data class NivelMapa(
    val id: Int,
    val orden: Int,
    val nombre: String,
    val tipoReto: String,          // texto del backend; usar `reto` en el código
    val descripcion: String?,
    val estado: String,            // texto del backend; usar `estadoNivel` en el código
    val puntos: Int = 0
) {
    // Se guardan como texto y se convierten aquí: si el backend agrega un valor nuevo, la app no se cae
    val reto: TipoReto get() = TipoReto.desde(tipoReto)
    val estadoNivel: EstadoNivel get() = EstadoNivel.desde(estado)
    val bloqueado: Boolean get() = estadoNivel == EstadoNivel.BLOQUEADO
}

// Mapa completo del alumno, con el resumen para la cabecera
data class MapaRespuesta(
    val estudianteId: Long,
    val nombres: String,
    val apellidos: String,
    val totalNiveles: Int,
    val completados: Int,
    val puntos: Int,
    val niveles: List<NivelMapa>
)