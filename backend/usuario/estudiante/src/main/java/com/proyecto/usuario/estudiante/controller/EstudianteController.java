package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.PerfilDTO;
import com.proyecto.usuario.estudiante.exception.CodigoAulaInvalidoException;
import com.proyecto.usuario.estudiante.repository.EstudianteRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estudiante")
public class EstudianteController {

    private final EstudianteRepository estudiantes;

    public EstudianteController(EstudianteRepository estudiantes) {
        this.estudiantes = estudiantes;
    }

    // Perfiles del aula para "¿Quién eres?". El id es el que se usa luego en el login.
    @GetMapping("/perfiles")
    public List<PerfilDTO> listarPerfiles(@RequestParam String codigoAula) {
        String codigo = codigoAula.trim();
        if (!estudiantes.existeAula(codigo)) {
            throw new CodigoAulaInvalidoException("El código de aula no existe");
        }
        return estudiantes.listarActivosPorCodigoAula(codigo).stream()
                .map(e -> new PerfilDTO(e.id(), e.nombre(), e.avatarUrl()))
                .toList();
    }
}