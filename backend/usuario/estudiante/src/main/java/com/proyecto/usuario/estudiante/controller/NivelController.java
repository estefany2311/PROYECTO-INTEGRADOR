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

    @GetMapping("/{nivelId}/lectura")
    public LecturaNivelResponse lectura(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return lecturaService.lecturaDelNivel(estudianteId, nivelId);
    }

    @GetMapping("/{nivelId}/consigna")
    public ConsignaNivelResponse consigna(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return consignaService.consignaDelNivel(estudianteId, nivelId);
    }

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
