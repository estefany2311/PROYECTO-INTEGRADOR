package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.LibroResponse;
import com.proyecto.usuario.docente.service.BibliotecaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Biblioteca: GET /api/docente/libros?grado=3 (el grado es opcional). */
@RestController
@RequestMapping("/api/docente/libros")
@Tag(name = "Biblioteca", description = "Libros oficiales del Minedu.")
@SecurityRequirement(name = "docenteId")
public class BibliotecaController {

    private final BibliotecaService service;

    public BibliotecaController(BibliotecaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista los libros oficiales (filtro opcional: grado)")
    public List<LibroResponse> listar(@RequestHeader("X-Docente-Id") Long docenteId,
                                      @RequestParam(required = false) Integer grado) {
        return service.libros(grado);
    }
}
