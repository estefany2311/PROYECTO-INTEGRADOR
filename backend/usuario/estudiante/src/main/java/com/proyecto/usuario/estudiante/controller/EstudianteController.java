package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.PerfilDTO;
import com.proyecto.usuario.estudiante.exception.CodigoAulaInvalidoException;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/estudiante")
public class EstudianteController {

    private static final String CODIGO_AULA_DEMO = "DEMO123";

    @GetMapping("/perfiles")
    public List<PerfilDTO> listarPerfiles(@RequestParam String codigoAula) {
        if (!CODIGO_AULA_DEMO.equalsIgnoreCase(codigoAula)) {
            throw new CodigoAulaInvalidoException("El código de aula no existe");
        }
        return List.of(
                new PerfilDTO(1, "Ana", "https://i.pravatar.cc/150?img=1"),
                new PerfilDTO(2, "Luis", "https://i.pravatar.cc/150?img=2"),
                new PerfilDTO(3, "Mía", "https://i.pravatar.cc/150?img=3"),
                new PerfilDTO(4, "Pedro", "https://i.pravatar.cc/150?img=4"),
                new PerfilDTO(5, "Sofía", "https://i.pravatar.cc/150?img=5"),
                new PerfilDTO(6, "Iker", "https://i.pravatar.cc/150?img=6")
        );
    }
}