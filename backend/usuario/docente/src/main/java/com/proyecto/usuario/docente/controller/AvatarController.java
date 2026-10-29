package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AvatarResponse;
import com.proyecto.usuario.docente.repository.AvatarRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docente/avatares")
@CrossOrigin(origins = "http://localhost:5173")
public class AvatarController {

    private final AvatarRepository avatarRepository;

    public AvatarController(AvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    // T-185: los 12 avatares, en orden, para el selector del portal docente
    @GetMapping
    public List<AvatarResponse> listar() {
        return avatarRepository.findAll(Sort.by("id")).stream()
                .map(a -> new AvatarResponse(a.getId(), a.getNombre(), a.getUrl()))
                .toList();
    }
}