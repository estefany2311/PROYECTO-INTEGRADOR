package com.proyecto.usuario.docente.dto;

/**
 * T-302 (US-09): acumulado de puntos y estrellas de un alumno después de
 * completar un reto. Lo reutiliza la respuesta del mapa en T-303.
 */
public record PuntajeEstudianteResponse(
        Long estudianteId,
        int puntos,
        int estrellas
) {
}
