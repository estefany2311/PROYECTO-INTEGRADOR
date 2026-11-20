package com.proyecto.usuario.estudiante.dto;

// T-416: la app pregunta cada tanto si la IA ya escuchó la lectura (procesando, listo o error)
public record EstadoEvidenciaResponse(
        long evidenciaId,
        String estado,
        String transcripcion,
        String mensaje
) {}
