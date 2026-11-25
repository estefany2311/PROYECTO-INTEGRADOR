package com.proyecto.usuario.docente.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * T-371 (US-32): detalle de una evidencia para que el docente contraste la
 * evaluación automática.
 *
 * <p>Reúne el contenido que produjo el alumno ({@code audioUrl} o {@code texto}),
 * lo que la IA entendió del audio ({@code transcripcion}), las palabras que marcó
 * para revisar ({@code palabrasMarcadas}), su puntaje global
 * ({@code puntajeIa}, de 0 a 100) y la retroalimentación ({@code resultadoIa}).
 * Incluye también la revisión del docente: la {@code calificacionFinal} que deja
 * al aprobar o corregir, el {@code motivoCorreccion} y el estado de revisión.
 * T-380 (US-33) agrega el {@code historial} de revisiones (nota IA vs. nota
 * docente), del más reciente al más antiguo. T-384 (US-34) agrega el
 * {@code comentario} de retroalimentación. Las fechas vienen resueltas para
 * que la pantalla no haga llamadas adicionales.</p>
 */
public record EvidenciaDetalleResponse(
        Long id,
        Long estudianteId,
        String nombres,
        String apellidos,
        Short gradoId,
        String gradoNombre,
        Short nivelOrden,
        String nivelNombre,
        String tipoReto,
        String tipo,
        String audioUrl,
        String texto,
        String transcripcion,
        List<String> palabrasMarcadas,
        Short puntajeIa,
        Short calificacionFinal,
        String motivoCorreccion,
        String comentario,
        String resultadoIa,
        String estadoRevision,
        LocalDateTime creadoEn,
        LocalDateTime revisadoEn,
        List<HistorialCalificacionResponse> historial
) {
}
