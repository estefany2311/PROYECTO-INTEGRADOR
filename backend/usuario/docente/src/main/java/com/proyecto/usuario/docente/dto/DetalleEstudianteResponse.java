package com.proyecto.usuario.docente.dto;

import com.proyecto.usuario.docente.model.ColorSemaforo;

import java.util.List;

/**
 * T-290 (US-29): detalle de progreso de un estudiante.
 * Reúne el mapa de niveles, los puntajes por competencia y las últimas
 * evaluaciones para que la pantalla de detalle (T-292) no haga varias llamadas.
 */
public record DetalleEstudianteResponse(
        Long estudianteId,
        String nombres,
        String apellidos,
        Short gradoId,
        String gradoNombre,
        Integer promedioGeneral,
        ColorSemaforo colorGeneral,
        List<NivelMapaResponse> niveles,
        List<PuntajeCompetenciaResponse> puntajesPorCompetencia,
        List<EvidenciaEstudianteResponse> ultimasEvidencias
) {
}
