package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.CompletarNivelRequest;
import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.NivelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * T-214 (US-07): GET /api/estudiante/niveles/{nivelId} con "Authorization: Bearer <token>".
 *   200 detalle del nivel (tipo de reto, descripción, estado, puntos)
 *   401 sin token o con la sesión vencida
 *   403 el nivel está bloqueado para este alumno
 *   404 el nivel no existe o está desactivado
 * T-198: POST /api/estudiante/niveles/{nivelId}/completar (ver abajo)
 */
@RestController
@RequestMapping("/api/estudiante/niveles")
public class NivelController {

    private final AutenticadorTablet autenticador;
    private final NivelService nivelService;

    public NivelController(AutenticadorTablet autenticador, NivelService nivelService) {
        this.autenticador = autenticador;
        this.nivelService = nivelService;
    }

    @GetMapping("/{nivelId}")
    public NivelDetalleResponse detalle(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return nivelService.detalle(estudianteId, nivelId);
    }

    /**
     * T-198 (US-08): POST /api/estudiante/niveles/{nivelId}/completar con el token y {"puntos": 10}.
     *   200 nivel completado y siguiente desbloqueado
     *   400 puntos fuera de rango · 401 sin sesión · 403 nivel bloqueado · 404 no existe
     * El cuerpo es opcional: sin él cuenta como 0 puntos.
     */
    @PostMapping("/{nivelId}/completar")
    public CompletarNivelResponse completar(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody(required = false) CompletarNivelRequest request) {
        long estudianteId = autenticador.estudianteDe(authorization);
        int puntos = request == null ? 0 : request.puntosOCero();
        return nivelService.completar(estudianteId, nivelId, puntos);
    }
}
