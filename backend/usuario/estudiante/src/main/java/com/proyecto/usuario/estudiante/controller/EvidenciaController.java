package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.EstadoEvidenciaResponse;
import com.proyecto.usuario.estudiante.dto.ResultadoLecturaResponse;
import com.proyecto.usuario.estudiante.exception.EvidenciaNoEncontradaException;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

// T-416 (US-13): estado de la transcripción; T-425 (US-14): resultado de la lectura
@RestController
@RequestMapping("/api/estudiante/evidencias")
public class EvidenciaController {

    private final AutenticadorTablet autenticador;
    private final EvidenciaRepository evidencias;

    public EvidenciaController(AutenticadorTablet autenticador, EvidenciaRepository evidencias) {
        this.autenticador = autenticador;
        this.evidencias = evidencias;
    }

    @GetMapping("/{evidenciaId}/estado")
    public EstadoEvidenciaResponse estado(
            @PathVariable long evidenciaId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return evidencias.estado(evidenciaId, estudianteId).orElseThrow(EvidenciaNoEncontradaException::new);
    }

    // T-425: precisión, estrellas, puntos y nivel desbloqueado; mientras procesa llega con estado "procesando"
    @GetMapping("/{evidenciaId}/resultado")
    public ResultadoLecturaResponse resultado(
            @PathVariable long evidenciaId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return evidencias.resultado(evidenciaId, estudianteId).orElseThrow(EvidenciaNoEncontradaException::new);
    }
}
