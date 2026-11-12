package com.proyecto.usuario.docente.dto;

import java.util.List;

/**
 * T-197: mapa completo del alumno.
 * T-303 (US-09): el resumen incluye los puntos y las estrellas acumulados que
 * muestra la cabecera del mapa en la tablet.
 */
public record MapaResponse(
        Long estudianteId,
        String nombres,
        String apellidos,
        int totalNiveles,
        int completados,
        int puntos,
        int estrellas,
        List<NivelMapaResponse> niveles
) {
}
