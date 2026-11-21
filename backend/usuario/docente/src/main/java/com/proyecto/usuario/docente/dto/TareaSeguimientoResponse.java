package com.proyecto.usuario.docente.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * T-389 (US-38): una tarea asignada vista desde el seguimiento del docente.
 *
 * <p>{@code estado} es {@code pendiente}, {@code entregada} o {@code vencida}.
 * {@code entregadaATiempo} solo tiene valor cuando la tarea está entregada:
 * es verdadero si no tenía fecha límite o se entregó hasta ese día.</p>
 */
public record TareaSeguimientoResponse(
        Long id,
        Long lecturaId,
        String lecturaTitulo,
        Long estudianteId,
        String nombres,
        String apellidos,
        String gradoNombre,
        LocalDateTime fechaAsignacion,
        LocalDate fechaLimite,
        String estado,
        LocalDateTime entregadaEn,
        Boolean entregadaATiempo
) {
}
