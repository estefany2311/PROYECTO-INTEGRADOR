package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.MapaResponse;
import com.proyecto.usuario.docente.service.MapaEstudianteService;
import org.springframework.web.bind.annotation.*;

/**
 * T-197 (US-05): endpoint que consume la tablet de Karla.
 * GET /api/estudiante/{id}/mapa
 * Sin login todavía: el id del alumno viaja en la ruta, igual que el X-Docente-Id es provisional.
 */
@RestController
@RequestMapping("/api/estudiante/{estudianteId}/mapa")
@CrossOrigin(origins = "http://localhost:5173")
public class MapaEstudianteController {

    private final MapaEstudianteService service;

    public MapaEstudianteController(MapaEstudianteService service) {
        this.service = service;
    }

    @GetMapping
    public MapaResponse obtenerMapa(@PathVariable Long estudianteId) {
        return service.obtenerMapa(estudianteId);
    }
}
