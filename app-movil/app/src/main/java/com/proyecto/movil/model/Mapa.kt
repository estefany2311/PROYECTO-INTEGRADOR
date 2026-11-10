package com.proyecto.movil.model

// Tipos de reto de cada nivel. El backend los manda en minúsculas: "lectura", "escritura", "oralidad"
enum class TipoReto {
    LECTURA, ESCRITURA, ORALIDAD, DESCONOCIDO;

    companion object {
        fun desde(valor: String?): TipoReto =
            entries.firstOrNull { it.name.equals(valor, ignoreCase = true) } ?: DESCONOCIDO
    }
}


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