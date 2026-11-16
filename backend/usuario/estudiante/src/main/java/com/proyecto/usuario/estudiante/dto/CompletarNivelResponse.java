package com.proyecto.usuario.estudiante.dto;

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
