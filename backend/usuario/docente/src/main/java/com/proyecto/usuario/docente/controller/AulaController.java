package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AulaRequest;
import com.proyecto.usuario.docente.dto.AulaResponse;
import com.proyecto.usuario.docente.service.AulaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docente")
public class AulaController {

    private final AulaService service;

    public AulaController(AulaService service) {
        this.service = service;
    }

    // Endpoint POST para registrar aulas (retorna HTTP 201 Created)
    @PostMapping("/aulas")
    @ResponseStatus(HttpStatus.CREATED)
    public AulaResponse crear(
            @RequestHeader("X-Docente-Id") Long docenteId, // ID del docente desde el Header, El docente se identifica por ahora con el encabezado X-Docente-Id, de forma provisional, porque el login todavía no existe.
            @RequestBody AulaRequest req) {                // Datos del aula desde el Body (JSON)

        return service.crear(docenteId, req);
    }
}