package com.proyecto.usuario.estudiante.dto;

public record NivelDetalleResponse(
        int id,
        int orden,
        String nombre,
        String tipoReto,
        String descripcion,
        String estado,
        int puntos
) {}
