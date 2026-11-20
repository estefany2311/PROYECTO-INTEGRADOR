package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.EstadoEvidenciaResponse;
import com.proyecto.usuario.estudiante.exception.EvidenciaNoEncontradaException;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

// T-416 (US-13): estado de la transcripción de una grabación del alumno
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
}
