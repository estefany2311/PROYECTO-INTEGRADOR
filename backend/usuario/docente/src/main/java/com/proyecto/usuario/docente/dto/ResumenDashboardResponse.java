package com.proyecto.usuario.docente.dto;

/**
 * T-278 (US-27): conteo de alumnos por color del semáforo.
 * El total incluye a todos los alumnos activos del aula.
 */
public record ResumenDashboardResponse(
        int totalEstudiantes,
        int enNivelEsperado,
        int enProceso,
        int necesitanRefuerzo,
        int sinEvaluaciones
) {
}
