package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.NivelService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * T-214 (US-07): GET /api/estudiante/niveles/{nivelId} con "Authorization: Bearer <token>".
 *   200 detalle del nivel (tipo de reto, descripción, estado, puntos)
 *   401 sin token o con la sesión vencida
 *   403 el nivel está bloqueado para este alumno
 *   404 el nivel no existe o está desactivado
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
}
