package com.proyecto.usuario.estudiante.dto;

// T-425 (US-14): lo que muestra la pantalla de resultado de la app (T-426)
public record ResultadoLecturaResponse(
        long evidenciaId,
        int nivelId,
        String estado,                   // procesando, listo o error
        String transcripcion,
        Integer precision,
        Integer correctas,
        Integer evaluadas,
        Integer cobertura,
        Integer umbral,
        Boolean aprobado,
        Integer estrellas,
        Integer puntos,
        Integer nivelDesbloqueadoId,     // si con esta lectura se abrió el siguiente nivel por primera vez
        String nivelDesbloqueadoNombre,
        Long asignacionId                // si era una tarea del docente: la app la marca como entregada (US-38)
) {}
