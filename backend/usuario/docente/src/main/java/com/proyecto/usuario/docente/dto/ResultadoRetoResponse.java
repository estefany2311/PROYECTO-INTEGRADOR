package com.proyecto.usuario.docente.dto;

/**
 * Resultado de un reto de voz (lectura u oralidad) tal como lo lee la app del
 * alumno, con lo que sumó al total.
 *
 * @param tipoReto          {@code lectura} u {@code oralidad}
 * @param nota              nota de 0 a 100 de este intento (la calificación final
 *                          del docente si la hay)
 * @param puntos            puntos que da esa nota (puntos máximos × nota / 100)
 * @param estrellas         estrellas que da esa nota (0 a 3)
 * @param puntosTotales     puntos acumulados del alumno
 * @param estrellasTotales  estrellas acumuladas del alumno
 */
public record ResultadoRetoResponse(
        Long evidenciaId,
        String tipoReto,
        int nota,
        int puntos,
        int estrellas,
        int puntosTotales,
        int estrellasTotales
) {
}
