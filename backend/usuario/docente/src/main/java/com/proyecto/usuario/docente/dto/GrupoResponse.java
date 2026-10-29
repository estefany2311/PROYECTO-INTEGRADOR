package com.proyecto.usuario.docente.dto;

import java.util.List;

/**
 * T-195 (US-37): grupo tal como lo ve el docente, con sus alumnos.
 */
public record GrupoResponse(
        Long id,
        String nombre,
        String descripcion,
        List<GrupoAlumnoResponse> alumnos
) {
}
