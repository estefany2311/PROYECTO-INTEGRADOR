package com.proyecto.usuario.estudiante.dto;

/**
 * T-214 (US-07): lo que muestra la tablet al tocar un nivel disponible.
 * tipoReto: "lectura" | "escritura" | "oralidad". estado: "desbloqueado" | "completado".
 */
public record NivelDetalleResponse(
        int id,
        int orden,
        String nombre,
        String tipoReto,
        String descripcion,
        String estado,
        int puntos
) {}
