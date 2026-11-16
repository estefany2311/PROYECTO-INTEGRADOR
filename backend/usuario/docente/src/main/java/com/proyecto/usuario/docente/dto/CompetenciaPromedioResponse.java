package com.proyecto.usuario.docente.dto;

/**
 * T-278 (US-27): promedio de una competencia para el dashboard del aula.
 * Se usa tanto en la tarjeta del estudiante como en el resumen del aula.
 */
public record CompetenciaPromedioResponse(
        String competencia,
        double promedio
) {
}
