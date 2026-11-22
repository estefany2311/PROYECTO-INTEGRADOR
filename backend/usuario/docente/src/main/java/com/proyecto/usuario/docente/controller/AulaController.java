package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AulaRequest;
import com.proyecto.usuario.docente.dto.AulaResponse;
import com.proyecto.usuario.docente.dto.GradoResponse;
import com.proyecto.usuario.docente.service.AulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador principal para endpoints del docente
@RestController
@RequestMapping("/api/docente")
@Tag(name = "Aulas", description = "Registro y listado de aulas multigrado del docente.")
public class AulaController {

    private final AulaService service;

    // Inyección del servicio de aulas
    public AulaController(AulaService service) {
        this.service = service;
    }

    // Endpoint para registrar un aula (POST /api/docente/aulas)
    @PostMapping("/aulas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un aula con sus grados")
    @SecurityRequirement(name = "docenteId")
    public AulaResponse crear(@RequestHeader("X-Docente-Id") Long docenteId,
                              @Valid @RequestBody AulaRequest req) {
        return service.crear(docenteId, req);
    }

    // Endpoint para obtener la lista de aulas del docente autenticado (GET /api/docente/aulas)
    @GetMapping("/aulas")
    @Operation(summary = "Lista las aulas del docente")
    @SecurityRequirement(name = "docenteId")
    public List<AulaResponse> listar(@RequestHeader("X-Docente-Id") Long docenteId) {
        return service.listar(docenteId);
    }

    // Endpoint público/general para obtener el catálogo de grados (GET /api/docente/grados)
    @GetMapping("/grados")
    @Operation(summary = "Lista los grados disponibles (no requiere header)")
    public List<GradoResponse> grados() {
        return service.listarGrados();
    }
}
