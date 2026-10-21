package com.proyecto.usuario.estudiante.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InicioSesionRequest(
        @NotNull(message = "Falta el estudiante") Long estudianteId,
        @NotNull(message = "Falta el PIN")
        @Pattern(regexp = "\\d{4}", message = "El PIN debe tener 4 números") String pin) {

    // Evita que el PIN aparezca si alguien imprime el objeto en un log
    @Override
    public String toString() {
        return "InicioSesionRequest[estudianteId=" + estudianteId + ", pin=****]";
    }
}