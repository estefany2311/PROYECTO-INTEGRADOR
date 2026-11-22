package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.DashboardAulaResponse;
import com.proyecto.usuario.docente.service.RendimientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
 *
 * T-286 (US-28): {@code ?gradoId=1&competencia=lectura} filtra los alumnos,
 * el resumen y los promedios por el grado y la competencia indicados.
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/dashboard")
@Tag(name = "Dashboard", description = "Rendimiento del aula por semáforo, grado y competencia.")
@SecurityRequirement(name = "docenteId")
public class DashboardAulaController {

    private final RendimientoService service;

    public DashboardAulaController(RendimientoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Dashboard del aula (filtros opcionales: gradoId, competencia, soloRojo)")
    public DashboardAulaResponse obtener(@RequestHeader("X-Docente-Id") Long docenteId,
                                         @PathVariable Long aulaId,
                                         @RequestParam(required = false) Short gradoId,
                                         @RequestParam(required = false) String competencia,
                                         @RequestParam(defaultValue = "false") boolean soloRojo) {
        return service.dashboardAula(docenteId, aulaId, gradoId, competencia, soloRojo);
    }
}
