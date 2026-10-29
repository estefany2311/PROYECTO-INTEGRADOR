package com.proyecto.usuario.docente.dto;

import java.util.List;

/**
 * T-197: mapa completo del alumno.
 * El resumen (total, completados, puntos) lo usa la cabecera del mapa en la tablet.
 */
public record MapaResponse(
        Long estudianteId,
        String nombres,
        String apellidos,
        int totalNiveles,
        int completados,
        int puntos,
        List<NivelMapaResponse> niveles
) {
}
