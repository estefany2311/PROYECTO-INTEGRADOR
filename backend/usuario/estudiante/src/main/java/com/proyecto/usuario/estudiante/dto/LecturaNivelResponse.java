package com.proyecto.usuario.estudiante.dto;

public record LecturaNivelResponse(
        int nivelId,
        long lecturaId,
        String titulo,
        String texto,
        int grado,
        String dificultad,
        boolean asignadaPorDocente
) {}
