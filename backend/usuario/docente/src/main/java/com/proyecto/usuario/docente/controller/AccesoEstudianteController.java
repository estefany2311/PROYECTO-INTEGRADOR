package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AsignarAvatarRequest;
import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.dto.PinGeneradoResponse;
import com.proyecto.usuario.docente.service.AccesoEstudianteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/estudiantes/{estudianteId}")
@CrossOrigin(origins = "http://localhost:5173",
        methods = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.OPTIONS})
public class AccesoEstudianteController {

    private final AccesoEstudianteService service;

    public AccesoEstudianteController(AccesoEstudianteService service) {
        this.service = service;
    }

    // el docente elige el avatar del alumno
    @PutMapping("/avatar")
    public EstudianteResponse asignarAvatar(@RequestHeader("X-Docente-Id") Long docenteId,
                                            @PathVariable Long aulaId,
                                            @PathVariable Long estudianteId,
                                            @Valid @RequestBody AsignarAvatarRequest req) {
        return service.asignarAvatar(docenteId, aulaId, estudianteId, req.avatarId());
    }

    // genera o restablece el PIN: la respuesta trae el PIN UNA sola vez
    @PostMapping("/pin")
    public PinGeneradoResponse generarPin(@RequestHeader("X-Docente-Id") Long docenteId,
                                          @PathVariable Long aulaId,
                                          @PathVariable Long estudianteId) {
        return service.generarPin(docenteId, aulaId, estudianteId);
    }
}