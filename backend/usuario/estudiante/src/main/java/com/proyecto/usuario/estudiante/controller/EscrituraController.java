package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.CompletarEscrituraService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

// T-435 (US-21): POST /api/estudiante/evidencias/{id}/completar · registra el reto de escritura ya calificado
@RestController
@RequestMapping("/api/estudiante/evidencias")
public class EscrituraController {

    private final AutenticadorTablet autenticador;
    private final CompletarEscrituraService service;

    public EscrituraController(AutenticadorTablet autenticador, CompletarEscrituraService service) {
        this.autenticador = autenticador;
        this.service = service;
    }

    @PostMapping("/{evidenciaId}/completar")
    public CompletarNivelResponse completar(
            @PathVariable long evidenciaId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return service.completar(estudianteId, evidenciaId);
    }
}
