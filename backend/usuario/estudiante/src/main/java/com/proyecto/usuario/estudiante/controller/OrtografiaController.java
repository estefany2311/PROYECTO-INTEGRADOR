package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.VerificarTextoRequest;
import com.proyecto.usuario.estudiante.dto.VerificarTextoResponse;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.OrtografiaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// T-328 (US-19): POST /api/estudiante/ortografia/verificar · 200 errores · 400 texto inválido · 401 sin sesión
@RestController
@RequestMapping("/api/estudiante/ortografia")
public class OrtografiaController {

    private final AutenticadorTablet autenticador;
    private final OrtografiaService service;

    public OrtografiaController(AutenticadorTablet autenticador, OrtografiaService service) {
        this.autenticador = autenticador;
        this.service = service;
    }

    @PostMapping("/verificar")
    public VerificarTextoResponse verificar(
            @Valid @RequestBody VerificarTextoRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return service.verificar(estudianteId, request.texto());
    }
}
