package com.proyecto.usuario.estudiante.dto;

import com.proyecto.usuario.estudiante.model.PalabraTexto;

import java.util.List;

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
        Long asignacionId,               // si era una tarea del docente: la app la marca como entregada (US-38)
        List<PalabraTexto> palabras      // T-428: el texto palabra por palabra; vacío mientras procesa
) {
    public ResultadoLecturaResponse conPalabras(List<PalabraTexto> nuevas) {
        return new ResultadoLecturaResponse(evidenciaId, nivelId, estado, transcripcion, precision, correctas,
                evaluadas, cobertura, umbral, aprobado, estrellas, puntos, nivelDesbloqueadoId,
                nivelDesbloqueadoNombre, asignacionId, nuevas);
    }
}
