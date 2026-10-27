package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.InicioSesionRequest;
import com.proyecto.usuario.estudiante.dto.SesionResponse;
import com.proyecto.usuario.estudiante.exception.TokenFaltanteException;
import com.proyecto.usuario.estudiante.service.SesionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estudiante/sesion")
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    // US-02: el estudiante elige su perfil y escribe su PIN; si es correcto recibe un token
    @PostMapping
    public SesionResponse iniciar(@Valid @RequestBody InicioSesionRequest request) {
        return sesionService.iniciarSesion(request.estudianteId(), request.pin());
    }

    /**
     * T-236 (US-04): DELETE /api/estudiante/sesion con "Authorization: Bearer <token>".
     * Responde 204 (sin cuerpo) y el token queda invalidado. Sin token responde 401.
     */
    @DeleteMapping
    public ResponseEntity<Void> cerrar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        sesionService.cerrarSesion(extraerToken(authorization));
        return ResponseEntity.noContent().build();
    }

    private static final String PREFIJO = "Bearer ";

    private static String extraerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(PREFIJO)) {
            throw new TokenFaltanteException();
        }
        String token = authorization.substring(PREFIJO.length()).trim();
        if (token.isEmpty()) {
            throw new TokenFaltanteException();
        }
        return token;
    }
}