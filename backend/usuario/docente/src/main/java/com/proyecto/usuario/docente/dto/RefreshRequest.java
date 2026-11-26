package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotBlank;

/** Login docente: token de renovación, para renovar el acceso o cerrar sesión. */
public record RefreshRequest(
        @NotBlank(message = "Falta el token de renovación")
        String refresh
) {
}
