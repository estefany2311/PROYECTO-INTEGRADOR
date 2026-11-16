package com.proyecto.usuario.docente.dto;

/**
 * T-189: una lectura del catálogo tal como la ve el docente.
 * gradoNombre ("1.º") viene de mi tabla grado para no mostrar solo el número.
 */
public record LecturaResponse(
        Long id,
        String titulo,
        String texto,
        String tipoReto,
        String dificultad,
        Short gradoId,
        String gradoNombre,
        boolean activo
) {
}
