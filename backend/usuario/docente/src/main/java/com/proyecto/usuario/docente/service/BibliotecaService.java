package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.LibroResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.repository.LibroRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/** Biblioteca: libros oficiales del Minedu disponibles para el docente. */
@Service
public class BibliotecaService {

    private final LibroRepository libroRepository;

    public BibliotecaService(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    public List<LibroResponse> libros(Integer grado) {
        if (grado != null && (grado < 1 || grado > 6)) {
            throw new ReglaNegocioException("El grado debe estar entre 1 y 6", HttpStatus.BAD_REQUEST);
        }
        return libroRepository.listar(grado);
    }
}
