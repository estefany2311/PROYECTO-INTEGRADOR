package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AccesoResponse;
import com.proyecto.usuario.docente.dto.LoginDocenteRequest;
import com.proyecto.usuario.docente.dto.RefreshRequest;
import com.proyecto.usuario.docente.dto.SesionDocenteResponse;
import com.proyecto.usuario.docente.seguridad.SesionDocenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Login docente: iniciar, renovar y cerrar la sesión del portal. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Acceso del docente", description = "Inicio de sesión con las cuentas del administrador.")
public class AuthDocenteController {

    private final SesionDocenteService service;

    public AuthDocenteController(SesionDocenteService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia la sesión de un docente")
    public SesionDocenteResponse iniciar(@Valid @RequestBody LoginDocenteRequest req) {
        return service.iniciar(req.username(), req.password());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva el token de acceso")
    public AccesoResponse renovar(@Valid @RequestBody RefreshRequest req) {
        return new AccesoResponse(service.renovar(req.refresh()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cierra la sesión y anula sus tokens")
    public void cerrar(@Valid @RequestBody RefreshRequest req) {
        service.cerrar(req.refresh());
    }
}
