package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.InicioSesionRequest;
import com.proyecto.usuario.estudiante.dto.SesionResponse;
import com.proyecto.usuario.estudiante.service.SesionService;
import jakarta.validation.Valid;
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
}