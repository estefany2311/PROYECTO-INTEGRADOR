package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.config.InterceptorTokenEstudiante;
import com.proyecto.usuario.docente.dto.MapaResponse;
import com.proyecto.usuario.docente.service.MapaEstudianteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * T-197 (US-05): endpoint que consume la tablet de Karla.
 * GET /api/estudiante/mapa  con  Authorization: Bearer <token>
 * El id del alumno sale del token (InterceptorTokenEstudiante), no de la URL.
 *
 * T-303 (US-09): la cabecera del mapa incluye puntos y estrellas acumulados.
 */
@RestController
@RequestMapping("/api/estudiante/mapa")
@Tag(name = "Mapa del alumno", description = "Mapa de niveles que consume la tablet (token del alumno).")
@SecurityRequirement(name = "tokenEstudiante")
public class MapaEstudianteController {

    private final MapaEstudianteService service;

    public MapaEstudianteController(MapaEstudianteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Mapa del alumno autenticado (niveles, puntos y estrellas)")
    public MapaResponse obtenerMapa(
            @RequestAttribute(InterceptorTokenEstudiante.ATRIBUTO_ESTUDIANTE) Long estudianteId) {
        return service.obtenerMapa(estudianteId);
    }
}
