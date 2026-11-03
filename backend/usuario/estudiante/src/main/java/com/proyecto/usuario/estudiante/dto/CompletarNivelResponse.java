package com.proyecto.usuario.estudiante.dto;

/**
 * T-198 (US-08): resultado de completar un nivel, para que la tablet refresque el mapa (T-216).
 * siguienteNivelId es null si era el último nivel del recorrido.
 */
public record CompletarNivelResponse(
        int nivelId,
        String estado,
        int puntos,
        Integer siguienteNivelId,
        String siguienteNivelNombre
) {}
