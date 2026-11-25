package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * T-384 (US-34): comentario de retroalimentación que el docente deja en una
 * evidencia.
 *
 * <p>El comentario es obligatorio y se guarda recortado. El límite de 500
 * caracteres evita textos desproporcionados en la app del alumno.</p>
 */
public record GuardarComentarioRequest(
        @NotBlank(message = "Debes escribir el comentario")
        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
        String comentario
) {
}
