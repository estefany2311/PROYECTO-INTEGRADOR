package com.proyecto.usuario.docente.dto;

import java.time.LocalDateTime;

/**
 * T-290 (US-29): una evaluación reciente del alumno.
 * La tabla de evidencias con audio/texto y estado de revisión llega en US-31
 * (T-296); por ahora el detalle muestra el historial de evaluaciones.
 */
public record EvidenciaEstudianteResponse(
        Long id,
        String competencia,
        short puntaje,
        LocalDateTime evaluadoEn
) {
}
