package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.LecturaResponse;
import com.proyecto.usuario.docente.service.CatalogoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * T-189 (US-35): catálogo del portal docente.
 * GET /api/docente/aulas/{aulaId}/lecturas
 *   ?gradoId=1&dificultad=basica&tipoReto=lectura   (todos los filtros son opcionales)
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/lecturas")
@CrossOrigin(origins = "http://localhost:5173",
        methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class CatalogoController {

    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    public List<LecturaResponse> listar(@RequestHeader("X-Docente-Id") Long docenteId,
                                        @PathVariable Long aulaId,
                                        @RequestParam(required = false) Short gradoId,
                                        @RequestParam(required = false) String dificultad,
                                        @RequestParam(required = false) String tipoReto) {
        return service.listar(docenteId, aulaId, gradoId, dificultad, tipoReto);
    }
}
//
