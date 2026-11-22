package com.proyecto.usuario.estudiante.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerificarTextoRequest(
        @NotNull(message = "Falta el texto")
        @Size(max = 3000, message = "El texto es demasiado largo")
        String texto
) {}
