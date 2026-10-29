package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AsignacionGrupoResponse;
import com.proyecto.usuario.docente.dto.AsignarGrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoResponse;
import com.proyecto.usuario.docente.service.GrupoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * T-195 (US-37): grupos del aula.
 * GET  /api/docente/aulas/{aulaId}/grupos
 * POST /api/docente/aulas/{aulaId}/grupos
 * POST /api/docente/aulas/{aulaId}/grupos/{grupoId}/asignaciones
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/grupos")
@CrossOrigin(origins = "http://localhost:5173",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class GrupoController {

    private final GrupoService service;

    public GrupoController(GrupoService service) {
        this.service = service;
    }

    @GetMapping
    public List<GrupoResponse> listar(@RequestHeader("X-Docente-Id") Long docenteId,
                                      @PathVariable Long aulaId) {
        return service.listar(docenteId, aulaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoResponse crear(@RequestHeader("X-Docente-Id") Long docenteId,
                               @PathVariable Long aulaId,
                               @Valid @RequestBody GrupoRequest req) {
        return service.crear(docenteId, aulaId, req);
    }

    @PostMapping("/{grupoId}/asignaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public AsignacionGrupoResponse asignar(@RequestHeader("X-Docente-Id") Long docenteId,
                                           @PathVariable Long aulaId,
                                           @PathVariable Long grupoId,
                                           @Valid @RequestBody AsignarGrupoRequest req) {
        return service.asignar(docenteId, aulaId, grupoId, req);
    }
}
