package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.CompletarNivelRequest;
import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.dto.ConsignaNivelResponse;
import com.proyecto.usuario.estudiante.dto.LecturaNivelResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.ConsignaService;
import com.proyecto.usuario.estudiante.service.LecturaService;
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
 * T-320: GET /api/estudiante/niveles/{nivelId}/consigna (ver abajo)
 */
@RestController
@RequestMapping("/api/estudiante/niveles")
public class NivelController {

    private final AutenticadorTablet autenticador;
    private final NivelService nivelService;
    private final LecturaService lecturaService;
    private final ConsignaService consignaService;

    public NivelController(AutenticadorTablet autenticador, NivelService nivelService,
                           LecturaService lecturaService, ConsignaService consignaService) {
        this.autenticador = autenticador;
        this.nivelService = nivelService;
        this.lecturaService = lecturaService;
        this.consignaService = consignaService;
    }

    @GetMapping("/{nivelId}")
    public NivelDetalleResponse detalle(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return nivelService.detalle(estudianteId, nivelId);
    }

    /**
     * T-218 (US-10): GET /api/estudiante/niveles/{nivelId}/lectura con el token.
     *   200 el texto que el alumno va a leer (asignado por su docente o del catálogo de su grado)
     *   401 sin sesión · 403 nivel bloqueado · 404 el nivel no existe o todavía no tiene lectura
     */
    @GetMapping("/{nivelId}/lectura")
    public LecturaNivelResponse lectura(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return lecturaService.lecturaDelNivel(estudianteId, nivelId);
    }

    /**
     * T-320 (US-17): GET /api/estudiante/niveles/{nivelId}/consigna con el token.
     *   200 la consigna del nivel de escritura (texto, pista, ilustración y mínimo de palabras)
     *   401 sin sesión · 403 nivel bloqueado · 404 el nivel no existe, no es de escritura o no tiene consigna
     */
    @GetMapping("/{nivelId}/consigna")
    public ConsignaNivelResponse consigna(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return consignaService.consignaDelNivel(estudianteId, nivelId);
    }

    /**
     * T-198 (US-08): POST /api/estudiante/niveles/{nivelId}/completar con el token y {"puntos": 10, "estrellas": 2}.
     *   200 con "aprobado": true si sacó las estrellas mínimas (nivel completado y siguiente desbloqueado, T-309),
     *       o "aprobado": false si no (solo queda el intento)
     *   400 puntos fuera de rango · 401 sin sesión · 403 nivel bloqueado · 404 no existe
     * El cuerpo es opcional: sin él cuenta como 0 puntos y 0 estrellas.
     */
    @PostMapping("/{nivelId}/completar")
    public CompletarNivelResponse completar(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody(required = false) CompletarNivelRequest request) {
        long estudianteId = autenticador.estudianteDe(authorization);
        int puntos = request == null ? 0 : request.puntosOCero();
        int estrellas = request == null ? 0 : request.estrellasOCero();
        return nivelService.completar(estudianteId, nivelId, puntos, estrellas);
    }
}
