package com.proyecto.usuario.docente.dto;

/**
 * T-396 (US-15): fluidez lectora de una lectura en voz alta, tal como la lee la
 * app del alumno.
 *
 * @param puntajeFluidez     de 0 a 100 (ritmo y pausas)
 * @param palabrasPorMinuto  ritmo real de lectura, redondeado
 * @param palabrasEsperadas  ritmo esperado para el grado (configuración US-51)
 * @param pausasLargas       pausas mayores a la tolerancia del grado
 * @param vacilaciones       pausas cortas que indican duda
 * @param estrellas          estrellas ganadas (0 a 3) según las reglas de puntuación
 * @param mensaje            retroalimentación según el puntaje
 */
public record FluidezResponse(
        Long evidenciaId,
        int puntajeFluidez,
        int palabrasPorMinuto,
        int palabrasEsperadas,
        int pausasLargas,
        int vacilaciones,
        int estrellas,
        String mensaje
) {
}
