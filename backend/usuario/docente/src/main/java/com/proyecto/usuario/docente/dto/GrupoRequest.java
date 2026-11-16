package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * T-195 (US-37): crear un grupo dentro de un aula.
 * `estudiantesIds` puede venir vacío: el grupo nace sin alumnos y la
 * asignación a un grupo vacío se rechaza con 400.
 */
public record GrupoRequest(
        @NotBlank(message = "El nombre del grupo es obligatorio")
        @Size(max = 60, message = "El nombre del grupo no puede superar 60 caracteres")
        String nombre,

        @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
        String descripcion,

        List<@NotNull(message = "El alumno no puede ser nulo") Long> estudiantesIds
) {
}
