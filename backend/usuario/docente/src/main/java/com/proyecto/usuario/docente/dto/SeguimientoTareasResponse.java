package com.proyecto.usuario.docente.dto;

import java.util.List;

/**
 * T-389 (US-38): seguimiento de las tareas del aula. El resumen cuenta todas
 * las tareas del aula; {@code tareas} respeta el filtro de estado si se pidió.
 */
public record SeguimientoTareasResponse(
        int pendientes,
        int entregadas,
        int vencidas,
        List<TareaSeguimientoResponse> tareas
) {
}
