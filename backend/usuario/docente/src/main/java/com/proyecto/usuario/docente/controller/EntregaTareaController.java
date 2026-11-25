package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.config.InterceptorTokenEstudiante;
import com.proyecto.usuario.docente.dto.AsignacionResponse;
import com.proyecto.usuario.docente.service.EntregaTareaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * T-389 (US-38): la app de Karla marca una tarea como entregada.
 * POST /api/estudiante/asignaciones/{asignacionId}/entregar con
 * Authorization: Bearer token. El id del alumno sale del token.
 */
@RestController
@RequestMapping("/api/estudiante/asignaciones")
@Tag(name = "Entrega de tareas", description = "El alumno entrega sus tareas asignadas (token del alumno).")
@SecurityRequirement(name = "tokenEstudiante")
public class EntregaTareaController {

    private final EntregaTareaService service;

    public EntregaTareaController(EntregaTareaService service) {
        this.service = service;
    }

    @PostMapping("/{asignacionId}/entregar")
    @Operation(summary = "Marca como entregada una tarea del alumno autenticado")
    public AsignacionResponse entregar(
            @RequestAttribute(InterceptorTokenEstudiante.ATRIBUTO_ESTUDIANTE) Long estudianteId,
            @PathVariable Long asignacionId) {
        return service.entregar(estudianteId, asignacionId);
    }
}
