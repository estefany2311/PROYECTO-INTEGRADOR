package com.proyecto.usuario.docente.dto;

/**
 * T-189: una lectura del catálogo tal como la ve el docente.
 * gradoNombre ("1.º") viene de mi tabla grado para no mostrar solo el número.
 * Catálogo: fuente ("manual" u "oficial") y urlRecurso (enlace al libro del Minedu,
 * vacío en las manuales) vienen de las columnas que agregó Admin.
 */
public record LecturaResponse(
        Long id,
        String titulo,
        String texto,
        String tipoReto,
        String dificultad,
        Short gradoId,
        String gradoNombre,
        boolean activo,
        String fuente,
        String urlRecurso
) {
}
