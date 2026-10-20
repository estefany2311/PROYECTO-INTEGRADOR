package com.proyecto.usuario.docente.dto;

public record EstudianteResponse(
        Long id,
        String nombres,
        String apellidos,
        Short gradoId,
        String gradoNombre,
        boolean activo
) {
}