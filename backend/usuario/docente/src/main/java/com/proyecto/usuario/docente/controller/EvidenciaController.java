package com.proyecto.usuario.docente.controller;

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
}
