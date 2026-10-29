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

/*
 * T-213 (US-06): estado de un nivel para el alumno, tal como lo manda el backend en "estado":
 *   "bloqueado"    → todavía no puede entrar (le falta completar el nivel anterior)
 *   "desbloqueado" → puede jugarlo ahora
 *   "completado"   → ya lo terminó (puede volver a jugarlo)
 * El backend docente (T-197) calcula el estado con el progreso del alumno; la app solo lo lee.
 */
enum class EstadoNivel {
    BLOQUEADO, DESBLOQUEADO, COMPLETADO;

    companion object {
        // Sin importar mayúsculas ni espacios. Si llega un valor desconocido o falta,
        // se trata como bloqueado: nunca se abre un nivel por error
        fun desde(valor: String?): EstadoNivel =
            entries.firstOrNull { it.name.equals(valor?.trim(), ignoreCase = true) } ?: BLOQUEADO
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

    // T-213: atajos para la pantalla (T-239 estilos, T-240 mensaje al tocar un bloqueado)
    val bloqueado: Boolean get() = estadoNivel == EstadoNivel.BLOQUEADO
    val completado: Boolean get() = estadoNivel == EstadoNivel.COMPLETADO
    // Se puede entrar si está desbloqueado o ya completado (para repetirlo)
    val disponible: Boolean get() = !bloqueado
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