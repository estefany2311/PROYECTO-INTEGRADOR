package com.proyecto.usuario.estudiante.dto;

// T-413/T-414: respuesta al subir la grabación de una lectura
public record AudioRecibidoResponse(
        Long evidenciaId,
        int nivelId,
        long lecturaId,
        long duracionMs,
        String estado
) {}
