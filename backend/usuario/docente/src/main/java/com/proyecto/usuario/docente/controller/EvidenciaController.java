package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.EvidenciaDetalleResponse;
import com.proyecto.usuario.docente.dto.EvidenciaPendienteResponse;
import com.proyecto.usuario.docente.service.EvidenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * T-298 (US-31): evidencias pendientes de revisión del aula.
 * GET /api/docente/aulas/{aulaId}/evidencias
 *
 * <p>Devuelve las evidencias en estado {@code pendiente} de la más reciente a
 * la más antigua. {@code ?tipo=audio} o {@code ?tipo=texto} limita la lista a
 * ese tipo; la pantalla de T-299 usa el filtro. Requiere X-Docente-Id.</p>
 *
 * <p>T-371 (US-32): GET /api/docente/aulas/{aulaId}/evidencias/{evidenciaId}
 * devuelve el detalle de una evidencia (audio o texto, transcripción y puntaje
 * de la IA) para contrastar la evaluación automática.</p>
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/evidencias")
@Tag(name = "Evidencias", description = "Evidencias de audio y texto pendientes de revisión.")
@SecurityRequirement(name = "docenteId")
public class EvidenciaController {

    private final EvidenciaService service;

    public EvidenciaController(EvidenciaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista las evidencias pendientes (filtro opcional: tipo=audio|texto)")
    public List<EvidenciaPendienteResponse> pendientes(
            @RequestHeader("X-Docente-Id") Long docenteId,
            @PathVariable Long aulaId,
            @RequestParam(required = false) String tipo) {
        return service.pendientes(docenteId, aulaId, tipo);
    }

    // T-371 (US-32): detalle de una evidencia del aula, en cualquier estado
    @GetMapping("/{evidenciaId}")
    @Operation(summary = "Detalle de una evidencia (contenido, transcripción, puntaje y resultado de la IA)")
    public EvidenciaDetalleResponse detalle(
            @RequestHeader("X-Docente-Id") Long docenteId,
            @PathVariable Long aulaId,
            @PathVariable Long evidenciaId) {
        return service.detalle(docenteId, aulaId, evidenciaId);
    }
}
