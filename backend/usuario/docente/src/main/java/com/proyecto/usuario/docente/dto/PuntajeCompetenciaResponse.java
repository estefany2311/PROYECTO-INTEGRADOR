package com.proyecto.usuario.docente.dto;

import com.proyecto.usuario.docente.model.ColorSemaforo;

/**
 * T-290 (US-29): resumen de una competencia en el detalle del estudiante.
 * El promedio se calcula con todas las evaluaciones de esa competencia y el
 * color reutiliza la regla del semáforo (docs/regla-semaforo.md).
 */
public record PuntajeCompetenciaResponse(
        String competencia,
        double promedio,
        int evaluaciones,
        int ultimoPuntaje,
        ColorSemaforo color
) {
}
