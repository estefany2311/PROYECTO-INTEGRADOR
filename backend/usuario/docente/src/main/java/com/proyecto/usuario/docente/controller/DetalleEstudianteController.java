package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.DetalleEstudianteResponse;
import com.proyecto.usuario.docente.service.DetalleEstudianteService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * T-290 (US-29): detalle de progreso de un estudiante.
 * GET /api/docente/aulas/{aulaId}/estudiantes/{estudianteId}/detalle
 *
 * Devuelve los niveles del mapa, los puntajes por competencia y las últimas
 * evaluaciones. Requiere el header X-Docente-Id.
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/estudiantes/{estudianteId}")
@CrossOrigin(origins = "http://localhost:5173",
        methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class DetalleEstudianteController {

    private final DetalleEstudianteService service;

    public DetalleEstudianteController(DetalleEstudianteService service) {
        this.service = service;
    }

    @GetMapping("/detalle")
    public DetalleEstudianteResponse detalle(@RequestHeader("X-Docente-Id") Long docenteId,
                                             @PathVariable Long aulaId,
                                             @PathVariable Long estudianteId) {
        return service.detalle(docenteId, aulaId, estudianteId);
    }
}
