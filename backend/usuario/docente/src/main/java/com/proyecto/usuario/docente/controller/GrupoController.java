package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AsignacionGrupoResponse;
import com.proyecto.usuario.docente.dto.AsignarGrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoResponse;
import com.proyecto.usuario.docente.service.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Grupos", description = "Grupos de alumnos y asignación de una lectura a todo el grupo.")
@SecurityRequirement(name = "docenteId")
public class GrupoController {

    private final GrupoService service;

    public GrupoController(GrupoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista los grupos del aula")
    public List<GrupoResponse> listar(@RequestHeader("X-Docente-Id") Long docenteId,
                                      @PathVariable Long aulaId) {
        return service.listar(docenteId, aulaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un grupo con alumnos del aula")
    public GrupoResponse crear(@RequestHeader("X-Docente-Id") Long docenteId,
                               @PathVariable Long aulaId,
                               @Valid @RequestBody GrupoRequest req) {
        return service.crear(docenteId, aulaId, req);
    }

    @PostMapping("/{grupoId}/asignaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Asigna una lectura a todos los alumnos del grupo")
    public AsignacionGrupoResponse asignar(@RequestHeader("X-Docente-Id") Long docenteId,
                                           @PathVariable Long aulaId,
                                           @PathVariable Long grupoId,
                                           @Valid @RequestBody AsignarGrupoRequest req) {
        return service.asignar(docenteId, aulaId, grupoId, req);
    }
}
