package com.proyecto.usuario.estudiante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// T-434 (US-21): el texto final que escribió el niño en el editor
public record EnviarTextoRequest(
        @NotBlank(message = "Escribe tu texto antes de enviarlo")
        @Size(max = 3000, message = "El texto es demasiado largo")
        String texto
) {}
