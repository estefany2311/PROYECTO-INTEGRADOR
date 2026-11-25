package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.EnviarTextoRequest;
import com.proyecto.usuario.estudiante.dto.TextoRecibidoResponse;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.TextoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// T-434 (US-21): POST /api/estudiante/niveles/{id}/texto · 201 evidencia · 400 texto inválido · 401 · 403 · 404
@RestController
@RequestMapping("/api/estudiante/niveles")
public class TextoController {

    private final AutenticadorTablet autenticador;
    private final TextoService textoService;

    public TextoController(AutenticadorTablet autenticador, TextoService textoService) {
        this.autenticador = autenticador;
        this.textoService = textoService;
    }

    @PostMapping("/{nivelId}/texto")
    @ResponseStatus(HttpStatus.CREATED)
    public TextoRecibidoResponse enviar(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody EnviarTextoRequest request) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return textoService.recibir(estudianteId, nivelId, request.texto());
    }
}
