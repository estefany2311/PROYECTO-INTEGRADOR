package com.proyecto.usuario.docente.dto;

/**
 * T-195 (US-37): alumno dentro de un grupo (solo lo que muestra la pantalla).
 */
public record GrupoAlumnoResponse(
        Long id,
        String nombres,
        String apellidos,
        String gradoNombre
) {
}
