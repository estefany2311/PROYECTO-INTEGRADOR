package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotNull;

/**
 * T-195 (US-37): lectura que se asigna a todos los alumnos del grupo.
 */
public record AsignarGrupoRequest(
        @NotNull(message = "Debes elegir la lectura")
        Long lecturaId
) {
}
