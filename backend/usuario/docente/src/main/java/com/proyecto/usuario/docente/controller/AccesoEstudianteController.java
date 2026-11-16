package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AsignarAvatarRequest;
import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.dto.PinGeneradoResponse;
import com.proyecto.usuario.docente.service.AccesoEstudianteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/estudiantes/{estudianteId}")
@Tag(name = "Acceso del alumno", description = "Avatar y PIN con el que el alumno entra a la tablet.")
@SecurityRequirement(name = "docenteId")
public class AccesoEstudianteController {

    private final AccesoEstudianteService service;

    public AccesoEstudianteController(AccesoEstudianteService service) {
        this.service = service;
    }

    // el docente elige el avatar del alumno
    @PutMapping("/avatar")
    @Operation(summary = "Asigna el avatar del alumno")
    public EstudianteResponse asignarAvatar(@RequestHeader("X-Docente-Id") Long docenteId,
                                            @PathVariable Long aulaId,
                                            @PathVariable Long estudianteId,
                                            @Valid @RequestBody AsignarAvatarRequest req) {
        return service.asignarAvatar(docenteId, aulaId, estudianteId, req.avatarId());
    }

    // genera o restablece el PIN: la respuesta trae el PIN UNA sola vez
    @PostMapping("/pin")
    @Operation(summary = "Genera o restablece el PIN del alumno")
    public PinGeneradoResponse generarPin(@RequestHeader("X-Docente-Id") Long docenteId,
                                          @PathVariable Long aulaId,
                                          @PathVariable Long estudianteId) {
        return service.generarPin(docenteId, aulaId, estudianteId);
    }
}
