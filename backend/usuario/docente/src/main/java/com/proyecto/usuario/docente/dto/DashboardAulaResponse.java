package com.proyecto.usuario.docente.dto;

import com.proyecto.usuario.docente.model.ColorSemaforo;

import java.util.List;

/**
 * T-278 (US-27): respuesta completa del dashboard de un aula.
 * Incluye el resumen por color, las tarjetas de los alumnos activos y los
 * promedios del aula por competencia.
 */
public record DashboardAulaResponse(
        Long aulaId,
        String aulaNombre,
        List<GradoResponse> grados,
        ResumenDashboardResponse resumen,
        List<EstudianteDashboardResponse> estudiantes,
        List<CompetenciaPromedioResponse> promediosPorCompetencia,
        Integer promedioGeneral,
        ColorSemaforo colorGeneral
) {
}
