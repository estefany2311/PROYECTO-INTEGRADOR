package com.proyecto.usuario.estudiante.dto;

/**
 * T-218 (US-10): el texto que el alumno lee en un nivel (pantalla de lectura, T-219).
 * asignadaPorDocente: true si la eligió su docente; false si salió del catálogo de su grado.
 */
public record LecturaNivelResponse(
        int nivelId,
        long lecturaId,
        String titulo,
        String texto,
        int grado,
        String dificultad,
        boolean asignadaPorDocente
) {}
