package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.MensajeDocenteResponse;
import com.proyecto.usuario.estudiante.repository.MensajeDocenteRepository;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// GET /api/estudiante/mensajes · los comentarios del docente para el alumno del token · 200 · 401
@RestController
@RequestMapping("/api/estudiante/mensajes")
public class MensajeController {

    private final AutenticadorTablet autenticador;
    private final MensajeDocenteRepository mensajes;

    public MensajeController(AutenticadorTablet autenticador, MensajeDocenteRepository mensajes) {
        this.autenticador = autenticador;
        this.mensajes = mensajes;
    }

    @GetMapping
    public List<MensajeDocenteResponse> listar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        long estudianteId = autenticador.estudianteDe(authorization);
        return mensajes.delEstudiante(estudianteId);
    }
}
