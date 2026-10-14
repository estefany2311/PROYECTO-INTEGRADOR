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
                new PerfilDTO(1, "Ana", "https://api.dicebear.com/9.x/adventurer/png?seed=Ana&backgroundColor=b6e3f4"),
                new PerfilDTO(2, "Luis", "https://api.dicebear.com/9.x/adventurer/png?seed=Luis&backgroundColor=ffd5dc"),
                new PerfilDTO(3, "Mía", "https://api.dicebear.com/9.x/adventurer/png?seed=Mia&backgroundColor=c0f5d8"),
                new PerfilDTO(4, "Pedro", "https://api.dicebear.com/9.x/adventurer/png?seed=Pedro&backgroundColor=ffe8b6"),
                new PerfilDTO(5, "Sofía", "https://api.dicebear.com/9.x/adventurer/png?seed=Sofia&backgroundColor=e0c3fc"),
                new PerfilDTO(6, "Iker", "https://api.dicebear.com/9.x/adventurer/png?seed=Iker&backgroundColor=b6e3f4")
        );
    }
}