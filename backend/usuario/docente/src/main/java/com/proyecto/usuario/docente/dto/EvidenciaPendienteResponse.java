package com.proyecto.usuario.docente.dto;

import java.time.LocalDateTime;

/**
 * T-298 (US-31): una evidencia pendiente de revisión en la lista del aula.
 * Reúne el alumno, el nivel, el contenido (audio o texto) y el resultado del
 * motor de IA para que la pantalla de T-299 no haga llamadas adicionales.
 */
public record EvidenciaPendienteResponse(
        Long id,
        Long estudianteId,
        String nombres,
        String apellidos,
        Short gradoId,
        String gradoNombre,
        Short nivelOrden,
        String nivelNombre,
        String tipo,
        String audioUrl,
        String texto,
        String resultadoIa,
        LocalDateTime creadoEn
) {
}
