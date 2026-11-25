package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.config.InterceptorTokenEstudiante;
import com.proyecto.usuario.docente.dto.ComentarioEvidenciaResponse;
import com.proyecto.usuario.docente.service.ComentarioEstudianteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * T-386 (US-34): endpoint que consume la app de Karla para mostrar al alumno el
 * comentario de su docente.
 * GET /api/estudiante/evidencias/{evidenciaId}/comentario con
 * Authorization: Bearer token. El id del alumno sale del token.
 */
@RestController
@RequestMapping("/api/estudiante/evidencias")
@Tag(name = "Comentario al alumno", description = "Comentario del docente que lee la tablet (token del alumno).")
@SecurityRequirement(name = "tokenEstudiante")
public class ComentarioEstudianteController {

    private final ComentarioEstudianteService service;

    public ComentarioEstudianteController(ComentarioEstudianteService service) {
        this.service = service;
    }

    @GetMapping("/{evidenciaId}/comentario")
    @Operation(summary = "Comentario del docente en una evidencia del alumno autenticado")
    public ComentarioEvidenciaResponse obtenerComentario(
            @RequestAttribute(InterceptorTokenEstudiante.ATRIBUTO_ESTUDIANTE) Long estudianteId,
            @PathVariable Long evidenciaId) {
        return service.obtenerComentario(estudianteId, evidenciaId);
    }
}
