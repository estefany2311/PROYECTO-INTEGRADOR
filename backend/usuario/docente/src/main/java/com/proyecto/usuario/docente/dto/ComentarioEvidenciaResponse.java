package com.proyecto.usuario.docente.dto;

/**
 * T-386 (US-34): comentario de retroalimentación que el docente dejó en una
 * evidencia, tal como lo lee la app del alumno.
 *
 * <p>{@code comentario} es {@code null} cuando el docente todavía no escribió
 * nada; la app decide si muestra o no el recuadro.</p>
 */
public record ComentarioEvidenciaResponse(
        Long evidenciaId,
        String comentario
) {
}
