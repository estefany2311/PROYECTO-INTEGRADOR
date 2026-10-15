package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AulaRequest;
import com.proyecto.usuario.docente.dto.AulaResponse;
import com.proyecto.usuario.docente.dto.GradoResponse;
import com.proyecto.usuario.docente.service.AulaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador principal para endpoints del docente
@RestController
@RequestMapping("/api/docente")
// Permite llamadas desde el entorno de desarrollo de React (CORS)
@CrossOrigin(origins = "http://localhost:5173")
public class AulaController {

    private final AulaService service;

    // Inyección del servicio de aulas
    public AulaController(AulaService service) {
        this.service = service;
    }

    // Endpoint para registrar un aula (POST /api/docente/aulas)
    @PostMapping("/aulas")
    @ResponseStatus(HttpStatus.CREATED)
    public AulaResponse crear(@RequestHeader("X-Docente-Id") Long docenteId,
                              @Valid @RequestBody AulaRequest req) {
        return service.crear(docenteId, req);
    }

    // Endpoint para obtener la lista de aulas del docente autenticado (GET /api/docente/aulas)
    @GetMapping("/aulas")
    public List<AulaResponse> listar(@RequestHeader("X-Docente-Id") Long docenteId) {
        return service.listar(docenteId);
    }

    // Endpoint público/general para obtener el catálogo de grados (GET /api/docente/grados)
    @GetMapping("/grados")
    public List<GradoResponse> grados() {
        return service.listarGrados();
    }
}