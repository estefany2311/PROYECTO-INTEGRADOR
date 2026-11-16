package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotNull;

/**
 * T-192 (US-36): datos para asignar una lectura del catálogo a un alumno.
 * El aula va en la ruta: /api/docente/aulas/{aulaId}/asignaciones
 */
public record AsignarLecturaRequest(
        @NotNull(message = "Debes elegir la lectura")
        Long lecturaId,

        @NotNull(message = "Debes elegir el alumno")
        Long estudianteId
) {
}
