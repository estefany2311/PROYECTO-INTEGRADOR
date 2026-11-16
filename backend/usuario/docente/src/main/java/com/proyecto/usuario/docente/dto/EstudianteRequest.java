package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EstudianteRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        String apellidos,

        @NotNull(message = "Debes seleccionar el grado del alumno")
        Short gradoId
) {
}