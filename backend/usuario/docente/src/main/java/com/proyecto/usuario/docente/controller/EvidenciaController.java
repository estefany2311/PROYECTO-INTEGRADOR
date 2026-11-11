package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.EvidenciaPendienteResponse;
import com.proyecto.usuario.docente.service.EvidenciaService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
@CrossOrigin(origins = "http://localhost:5173",
        methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class EvidenciaController {

    private final EvidenciaService service;

    public EvidenciaController(EvidenciaService service) {
        this.service = service;
    }

    @GetMapping
    public List<EvidenciaPendienteResponse> pendientes(
            @RequestHeader("X-Docente-Id") Long docenteId,
            @PathVariable Long aulaId,
            @RequestParam(required = false) String tipo) {
        return service.pendientes(docenteId, aulaId, tipo);
    }
}
