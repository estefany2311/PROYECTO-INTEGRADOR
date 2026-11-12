package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.LecturaResponse;
import com.proyecto.usuario.docente.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * T-189 (US-35): catálogo del portal docente.
 * GET /api/docente/aulas/{aulaId}/lecturas
 *   ?gradoId=1&dificultad=basica&tipoReto=lectura   (todos los filtros son opcionales)
 *
 * T-307: cada lectura incluye `fuente` (manual u oficial) y `urlRecurso`
 * (enlace al libro del Minedu, vacío en las manuales).
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/lecturas")
@Tag(name = "Catálogo", description = "Lecturas activas de los grados del aula, con filtros.")
@SecurityRequirement(name = "docenteId")
public class CatalogoController {

    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista las lecturas del aula (filtros opcionales: gradoId, dificultad, tipoReto)")
    public List<LecturaResponse> listar(@RequestHeader("X-Docente-Id") Long docenteId,
                                        @PathVariable Long aulaId,
                                        @RequestParam(required = false) Short gradoId,
                                        @RequestParam(required = false) String dificultad,
                                        @RequestParam(required = false) String tipoReto) {
        return service.listar(docenteId, aulaId, gradoId, dificultad, tipoReto);
    }
}
