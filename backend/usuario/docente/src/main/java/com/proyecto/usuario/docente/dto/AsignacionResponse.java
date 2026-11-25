package com.proyecto.usuario.docente.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * T-192 (US-36): asignación creada. La fecha la pone la base de datos.
 * T-388 (US-38): incluye la fecha límite (puede ser nula) y el estado guardado.
 */
public record AsignacionResponse(
        Long id,
        Long lecturaId,
        Long estudianteId,
        LocalDateTime fechaAsignacion,
        LocalDate fechaLimite,
        String estado
) {
}
