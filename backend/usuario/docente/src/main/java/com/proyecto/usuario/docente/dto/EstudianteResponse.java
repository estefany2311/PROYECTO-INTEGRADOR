package com.proyecto.usuario.docente.dto;

public record EstudianteResponse(
        Long id,
        String nombres,
        String apellidos,
        Short gradoId,
        String gradoNombre,
        boolean activo,
        Short avatarId,
        String avatarUrl,
        boolean tienePin   // el PIN nunca se envía, solo si ya tiene uno
) {
    // Constructor anterior de 6 campos: tus pruebas actuales siguen compilando
    public EstudianteResponse(Long id, String nombres, String apellidos,
                              Short gradoId, String gradoNombre, boolean activo) {
        this(id, nombres, apellidos, gradoId, gradoNombre, activo, null, null, false);
    }
}