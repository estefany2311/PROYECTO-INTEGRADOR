package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull(message = "Debes indicar el estado del alumno")
        Boolean activo
) {
}