package com.proyecto.usuario.estudiante.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * T-198 (US-08): lo que envía la tablet al terminar un reto.
 * puntos es opcional (si no llega, cuenta como 0). Las estrellas y su regla llegan en T-309.
 */
public record CompletarNivelRequest(
        @Min(value = 0, message = "Los puntos no pueden ser negativos")
        @Max(value = 1000, message = "Los puntos no pueden pasar de 1000")
        Integer puntos
) {
    public int puntosOCero() {
        return puntos == null ? 0 : puntos;
    }
}
