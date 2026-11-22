package com.proyecto.usuario.estudiante.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CompletarNivelRequest(
        @Min(value = 0, message = "Los puntos no pueden ser negativos")
        @Max(value = 1000, message = "Los puntos no pueden pasar de 1000")
        Integer puntos,

        @Min(value = 0, message = "Las estrellas van de 0 a 3")
        @Max(value = 3, message = "Las estrellas van de 0 a 3")
        Integer estrellas
) {
    public int puntosOCero() {
        return puntos == null ? 0 : puntos;
    }

    public int estrellasOCero() {
        return estrellas == null ? 0 : estrellas;
    }
}
