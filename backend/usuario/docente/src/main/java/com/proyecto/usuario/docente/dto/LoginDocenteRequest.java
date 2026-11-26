package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Login docente: usuario y contraseña de la cuenta que creó el administrador. */
public record LoginDocenteRequest(
        @NotBlank(message = "Escribe tu usuario")
        @Size(max = 150, message = "El usuario es demasiado largo")
        String username,

        @NotBlank(message = "Escribe tu contraseña")
        @Size(max = 128, message = "La contraseña es demasiado larga")
        String password
) {
    // la contraseña no debe aparecer si alguien imprime la petición
    @Override
    public String toString() {
        return "LoginDocenteRequest[username=" + username + "]";
    }
}
