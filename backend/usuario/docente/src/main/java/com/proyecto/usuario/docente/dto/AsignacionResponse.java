package com.proyecto.usuario.docente.dto;

import java.time.LocalDateTime;

/**
 * T-192 (US-36): asignación creada. La fecha la pone la base de datos.
 */
public record AsignacionResponse(
        Long id,
        Long lecturaId,
        Long estudianteId,
        LocalDateTime fechaAsignacion
) {
}
