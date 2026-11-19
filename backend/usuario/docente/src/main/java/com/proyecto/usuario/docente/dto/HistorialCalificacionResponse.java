package com.proyecto.usuario.docente.dto;

import java.time.LocalDateTime;

/**
 * T-380 (US-33): una revisión del docente dentro del historial de la evidencia.
 *
 * <p>{@code notaIa} es la nota que había dado la IA (nula si no la había),
 * {@code notaDocente} la que quedó tras aprobar o corregir, {@code motivo} solo
 * existe en las correcciones y {@code accion} es {@code aprobada} o
 * {@code corregida}.</p>
 */
public record HistorialCalificacionResponse(
        Long id,
        Long docenteId,
        Short notaIa,
        Short notaDocente,
        String motivo,
        String accion,
        LocalDateTime creadoEn
) {
}
