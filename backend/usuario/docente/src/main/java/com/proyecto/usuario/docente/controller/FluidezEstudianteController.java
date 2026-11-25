package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.config.InterceptorTokenEstudiante;
import com.proyecto.usuario.docente.dto.FluidezResponse;
import com.proyecto.usuario.docente.service.FluidezService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * T-397 (US-15): endpoint que consume la app de Karla para mostrar al alumno el
 * puntaje de fluidez de su lectura.
 * GET /api/estudiante/evidencias/{evidenciaId}/fluidez con
 * Authorization: Bearer token. El id del alumno sale del token.
 *
 * <p>Si el audio todavía se está transcribiendo responde 409 "Tu lectura todavía
 * se está procesando": la app puede volver a pedirlo en unos segundos.</p>
 */
@RestController
@RequestMapping("/api/estudiante/evidencias")
@Tag(name = "Fluidez lectora", description = "Puntaje de fluidez que lee la tablet (token del alumno).")
@SecurityRequirement(name = "tokenEstudiante")
public class FluidezEstudianteController {

    private final FluidezService service;

    public FluidezEstudianteController(FluidezService service) {
        this.service = service;
    }

    @GetMapping("/{evidenciaId}/fluidez")
    @Operation(summary = "Puntaje de fluidez (ritmo y pausas) de una lectura del alumno autenticado")
    public FluidezResponse obtenerFluidez(
            @RequestAttribute(InterceptorTokenEstudiante.ATRIBUTO_ESTUDIANTE) Long estudianteId,
            @PathVariable Long evidenciaId) {
        return service.calcular(estudianteId, evidenciaId);
    }
}
