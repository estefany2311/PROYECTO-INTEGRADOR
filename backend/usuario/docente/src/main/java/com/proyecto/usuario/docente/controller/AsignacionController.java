package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AsignacionResponse;
import com.proyecto.usuario.docente.dto.AsignarLecturaRequest;
import com.proyecto.usuario.docente.service.AsignacionService;
import com.proyecto.usuario.docente.dto.SeguimientoTareasResponse;
import com.proyecto.usuario.docente.service.SeguimientoTareasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * T-192 (US-36): POST /api/docente/aulas/{aulaId}/asignaciones
 * Body: { "lecturaId": 7, "estudianteId": 5 } → 201 con la asignación creada.
 *
 * T-389 (US-38): GET /api/docente/aulas/{aulaId}/asignaciones?estado=vencida
 * devuelve el seguimiento de las tareas (pendiente, entregada o vencida).
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/asignaciones")
@Tag(name = "Asignaciones", description = "Asigna una lectura del catálogo a un alumno concreto.")
@SecurityRequirement(name = "docenteId")
public class AsignacionController {

    private final AsignacionService service;
    private final SeguimientoTareasService seguimientoService;

    public AsignacionController(AsignacionService service,
                                SeguimientoTareasService seguimientoService) {
        this.service = service;
        this.seguimientoService = seguimientoService;
    }

    // T-389 (US-38): seguimiento de las tareas del aula
    @GetMapping
    @Operation(summary = "Seguimiento de las tareas del aula (filtro opcional: estado)")
    public SeguimientoTareasResponse seguimiento(@RequestHeader("X-Docente-Id") Long docenteId,
                                                 @PathVariable Long aulaId,
                                                 @RequestParam(required = false) String estado) {
        return seguimientoService.seguimiento(docenteId, aulaId, estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Asigna una lectura a un alumno")
    public AsignacionResponse asignar(@RequestHeader("X-Docente-Id") Long docenteId,
                                      @PathVariable Long aulaId,
                                      @Valid @RequestBody AsignarLecturaRequest req) {
        return service.asignar(docenteId, aulaId, req);
    }
}
