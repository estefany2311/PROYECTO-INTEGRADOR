package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.DashboardAulaResponse;
import com.proyecto.usuario.docente.service.RendimientoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * T-278 (US-27): dashboard de rendimiento del aula.
 * GET /api/docente/aulas/{aulaId}/dashboard
 *
 * Devuelve el resumen por color, una tarjeta por alumno activo y los
 * promedios del aula por competencia. Requiere el header X-Docente-Id.
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/dashboard")
@CrossOrigin(origins = "http://localhost:5173",
        methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class DashboardAulaController {

    private final RendimientoService service;

    public DashboardAulaController(RendimientoService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardAulaResponse obtener(@RequestHeader("X-Docente-Id") Long docenteId,
                                         @PathVariable Long aulaId) {
        return service.dashboardAula(docenteId, aulaId);
    }
}
