package com.proyecto.usuario.estudiante.dto;

/**
 * T-198 / T-309 (US-08): resultado de terminar un reto, para que la tablet refresque el mapa (T-216).
 *   aprobado: sacó las estrellas mínimas (T-309). Si es false, el nivel no se completa y no se desbloquea nada.
 *   estado: cómo queda el nivel para el alumno ("completado" o el que ya tenía).
 *   siguienteNivelId: el nivel que sigue y queda abierto; null si no aprobó o si era el último del recorrido.
 * T-310 (al repetir un reto):
 *   mejoresPuntos: el puntaje que queda guardado (el mejor de todos sus intentos).
 *   siguienteRecienDesbloqueado: true solo la primera vez que se abre el siguiente nivel.
 */
public record CompletarNivelResponse(
        int nivelId,
        String estado,
        int puntos,
        int mejoresPuntos,
        int estrellas,
        boolean aprobado,
        int estrellasMinimas,
        Integer siguienteNivelId,
        String siguienteNivelNombre,
        boolean siguienteRecienDesbloqueado
) {}
