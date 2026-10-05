package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AulaRequest;
import com.proyecto.usuario.docente.dto.AulaResponse;
import com.proyecto.usuario.docente.service.AulaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/docente")
public class AulaController {

    private final AulaService service;

    public AulaController(AulaService service) {
        this.service = service;
    }

    // Endpoint POST para registrar aulas con validación activada
    @PostMapping("/aulas")
    @ResponseStatus(HttpStatus.CREATED)
    public AulaResponse crear(
            @RequestHeader("X-Docente-Id") Long docenteId, // ID del docente desde el Header, El docente se identifica por ahora con el encabezado X-Docente-Id, de forma provisional, porque el login todavía no existe.
            @Valid @RequestBody AulaRequest req) {     // @Valid activa las validaciones anotadas en AulaRequest

        return service.crear(docenteId, req); // Llama al servicio para procesar el registro
    }
}