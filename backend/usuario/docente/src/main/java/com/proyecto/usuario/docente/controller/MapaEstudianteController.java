package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.config.InterceptorTokenEstudiante;
import com.proyecto.usuario.docente.dto.MapaResponse;
import com.proyecto.usuario.docente.service.MapaEstudianteService;
import org.springframework.web.bind.annotation.*;

/**
 * T-197 (US-05): endpoint que consume la tablet de Karla.
 * GET /api/estudiante/mapa  con  Authorization: Bearer <token>
 * El id del alumno sale del token (InterceptorTokenEstudiante), no de la URL.
 */
@RestController
@RequestMapping("/api/estudiante/mapa")
@CrossOrigin(origins = "http://localhost:5173")
public class MapaEstudianteController {

    private final MapaEstudianteService service;

    public MapaEstudianteController(MapaEstudianteService service) {
        this.service = service;
    }

    @GetMapping
    public MapaResponse obtenerMapa(
            @RequestAttribute(InterceptorTokenEstudiante.ATRIBUTO_ESTUDIANTE) Long estudianteId) {
        return service.obtenerMapa(estudianteId);
    }
}
