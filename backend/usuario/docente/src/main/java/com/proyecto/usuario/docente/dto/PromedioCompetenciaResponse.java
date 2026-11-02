package com.proyecto.usuario.docente.dto;

/**
 * T-276 (US-27): promedio de un alumno en una competencia.
 * El promedio va sin redondear; T-277 lo usa para calcular el semaforo.
 */
public record PromedioCompetenciaResponse(
        Long estudianteId,
        String competencia,
        double promedio
) {
}
