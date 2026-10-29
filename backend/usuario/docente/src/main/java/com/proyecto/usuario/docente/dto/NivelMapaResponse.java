package com.proyecto.usuario.docente.dto;

/**
 * T-197: un nivel tal como lo pinta el mapa de la tablet.
 * "estado" es bloqueado | desbloqueado | completado (US-06).
 */
public record NivelMapaResponse(
        Short id,
        Short orden,
        String nombre,
        String tipoReto,
        String descripcion,
        String estado,
        int puntos
) {
}
