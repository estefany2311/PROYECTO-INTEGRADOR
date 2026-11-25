package com.proyecto.usuario.docente.dto;

/**
 * T-400 (US-22): resultado de la escritura de un alumno, tal como lo lee la app.
 *
 * @param puntajeEscritura    puntaje final de 0 a 100
 * @param puntajeOrtografia   parte de ortografía (0 a 100)
 * @param puntajeLongitud     parte de longitud (0 a 100)
 * @param puntajeConsigna     parte de cumplimiento de la consigna (0 a 100)
 * @param palabras            palabras escritas
 * @param palabrasMinimas     palabras que pide la consigna del nivel
 * @param errores             palabras con error ortográfico
 * @param oraciones           oraciones del texto
 * @param oracionesCompletas  oraciones con mayúscula inicial y punto final
 * @param estrellas           estrellas ganadas (0 a 3) según las reglas de puntuación
 * @param mensaje             retroalimentación para el alumno según el puntaje
 */
public record ResultadoEscrituraResponse(
        Long evidenciaId,
        int puntajeEscritura,
        int puntajeOrtografia,
        int puntajeLongitud,
        int puntajeConsigna,
        int palabras,
        int palabrasMinimas,
        int errores,
        int oraciones,
        int oracionesCompletas,
        int estrellas,
        String mensaje
) {
}
