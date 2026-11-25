package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * T-192 (US-36): datos para asignar una lectura del catálogo a un alumno concreto.
 * El aula va en la ruta: /api/docente/aulas/{aulaId}/asignaciones
 *
 * <p>T-388 (US-38): {@code fechaLimite} es opcional; si viene, no puede estar
 * en el pasado.</p>
 */
public record AsignarLecturaRequest(
        @NotNull(message = "Debes elegir la lectura")
        Long lecturaId,

        @NotNull(message = "Debes elegir el alumno")
        Long estudianteId,

        @FutureOrPresent(message = "La fecha límite no puede estar en el pasado")
        LocalDate fechaLimite
) {
    // asignación sin fecha límite
    public AsignarLecturaRequest(Long lecturaId, Long estudianteId) {
        this(lecturaId, estudianteId, null);
    }
}
