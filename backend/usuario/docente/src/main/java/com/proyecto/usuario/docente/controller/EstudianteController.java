package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.EstudianteRequest;
import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.service.EstudianteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/estudiantes")
@CrossOrigin(origins = "http://localhost:5173")
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

    @GetMapping
    public List<EstudianteResponse> listar(@RequestHeader("X-Docente-Id") Long docenteId,
                                           @PathVariable Long aulaId) {
        return service.listar(docenteId, aulaId);
    }
}