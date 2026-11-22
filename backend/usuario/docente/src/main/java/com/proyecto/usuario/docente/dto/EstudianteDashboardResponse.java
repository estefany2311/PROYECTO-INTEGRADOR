package com.proyecto.usuario.docente.dto;

import com.proyecto.usuario.docente.model.ColorSemaforo;

import java.util.List;

/**
 * T-278 (US-27): tarjeta de rendimiento de un alumno en el dashboard.
 * El promedio general ya viene redondeado; el color se calculó con ese valor.
 * Un alumno sin evaluaciones tiene promedio nulo, color gris y la lista vacía.
 */
public record EstudianteDashboardResponse(
        Long id,
        String nombres,
        String apellidos,
        Short gradoId,
        String gradoNombre,
        Integer promedio,
        ColorSemaforo color,
        List<CompetenciaPromedioResponse> competencias
) {
}
