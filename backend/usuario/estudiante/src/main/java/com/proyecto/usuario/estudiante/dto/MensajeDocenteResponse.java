package com.proyecto.usuario.estudiante.dto;

import java.time.LocalDateTime;

// Comentario que el docente dejó en una evidencia del alumno (lo escribe el portal docente, US-34)
public record MensajeDocenteResponse(
        long evidenciaId,
        int nivelId,
        String nivelNombre,
        String tipo,                 // audio o texto
        String comentario,
        LocalDateTime fecha          // cuando lo revisó el docente; si no, cuando se envió la evidencia
) {}
