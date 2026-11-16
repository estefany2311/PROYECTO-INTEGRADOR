package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AsignacionResponse;
import com.proyecto.usuario.docente.dto.AsignarLecturaRequest;
import com.proyecto.usuario.docente.service.AsignacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * T-192 (US-36): POST /api/docente/aulas/{aulaId}/asignaciones
 * Body: { "lecturaId": 7, "estudianteId": 5 } → 201 con la asignación creada.
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/asignaciones")
@CrossOrigin(origins = "http://localhost:5173",
        methods = {RequestMethod.POST, RequestMethod.OPTIONS})
public class AsignacionController {

    private final AsignacionService service;

    public AsignacionController(AsignacionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AsignacionResponse asignar(@RequestHeader("X-Docente-Id") Long docenteId,
                                      @PathVariable Long aulaId,
                                      @Valid @RequestBody AsignarLecturaRequest req) {
        return service.asignar(docenteId, aulaId, req);
    }
}
