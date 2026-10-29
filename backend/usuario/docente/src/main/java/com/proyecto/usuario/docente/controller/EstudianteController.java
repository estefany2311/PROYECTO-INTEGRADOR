package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.EstadoRequest;
import com.proyecto.usuario.docente.dto.EstudianteRequest;
import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.service.EstudianteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/estudiantes")
@CrossOrigin(origins = "http://localhost:5173",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.PATCH, RequestMethod.OPTIONS})
public class EstudianteController {

    private final EstudianteService service;

    public EstudianteController(EstudianteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstudianteResponse registrar(@RequestHeader("X-Docente-Id") Long docenteId,
                                        @PathVariable Long aulaId,
                                        @Valid @RequestBody EstudianteRequest req) {
        return service.registrar(docenteId, aulaId, req);
    }

    // T-184: GET .../estudiantes → todos (portal docente)
    //        GET .../estudiantes?soloActivos=true → solo activos (tablet)
    @GetMapping
    public List<EstudianteResponse> listar(@RequestHeader("X-Docente-Id") Long docenteId,
                                           @PathVariable Long aulaId,
                                           @RequestParam(defaultValue = "false") boolean soloActivos) {
        return soloActivos
                ? service.listarActivos(docenteId, aulaId)
                : service.listar(docenteId, aulaId);
    }

    @PutMapping("/{estudianteId}")
    public EstudianteResponse actualizar(@RequestHeader("X-Docente-Id") Long docenteId,
                                         @PathVariable Long aulaId,
                                         @PathVariable Long estudianteId,
                                         @Valid @RequestBody EstudianteRequest req) {
        return service.actualizar(docenteId, aulaId, estudianteId, req);
    }

    @PatchMapping("/{estudianteId}/estado")
    public EstudianteResponse cambiarEstado(@RequestHeader("X-Docente-Id") Long docenteId,
                                            @PathVariable Long aulaId,
                                            @PathVariable Long estudianteId,
                                            @Valid @RequestBody EstadoRequest req) {
        return service.cambiarEstado(docenteId, aulaId, estudianteId, req.activo());
    }
}