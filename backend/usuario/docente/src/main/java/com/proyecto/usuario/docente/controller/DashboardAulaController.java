package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.DashboardAulaResponse;
import com.proyecto.usuario.docente.service.RendimientoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * T-278 (US-27): dashboard de rendimiento del aula.
 * GET /api/docente/aulas/{aulaId}/dashboard
 *
 * Devuelve el resumen por color, una tarjeta por alumno activo y los
 * promedios del aula por competencia. Requiere el header X-Docente-Id.
 *
 * T-282 (US-30): {@code ?soloRojo=true} devuelve solo las tarjetas rojas;
 * el resumen y los promedios siguen siendo del aula completa.
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
                                         @PathVariable Long aulaId,
                                         @RequestParam(defaultValue = "false") boolean soloRojo) {
        return service.dashboardAula(docenteId, aulaId, soloRojo);
    }
}
