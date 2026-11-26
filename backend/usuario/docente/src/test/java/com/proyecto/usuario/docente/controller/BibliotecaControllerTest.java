package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.LibroResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.repository.LibroRepository;
import com.proyecto.usuario.docente.service.BibliotecaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Biblioteca: lista de libros y filtro por grado
class BibliotecaControllerTest {

    private LibroRepository repositorio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        repositorio = mock(LibroRepository.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new BibliotecaController(new BibliotecaService(repositorio)))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void listaTodosLosLibros() throws Exception {
        when(repositorio.listar(null)).thenReturn(List.of(
                new LibroResponse(1L, "Libro de prueba", "Cuentos", 3, "lectura", true),
                new LibroResponse(2L, "Sin grado", "", null, "", false)));

        mvc.perform(get("/api/docente/libros").header("X-Docente-Id", 15))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Libro de prueba"))
                .andExpect(jsonPath("$[0].grado").value(3))
                .andExpect(jsonPath("$[0].tienePdf").value(true))
                .andExpect(jsonPath("$[1].grado").doesNotExist())
                .andExpect(jsonPath("$[1].tienePdf").value(false));
    }

    @Test
    void filtraPorGrado() throws Exception {
        when(repositorio.listar(3)).thenReturn(List.of());

        mvc.perform(get("/api/docente/libros").header("X-Docente-Id", 15).param("grado", "3"))
                .andExpect(status().isOk());
        verify(repositorio).listar(3);
    }

    @Test
    void unGradoFueraDeRangoDa400() throws Exception {
        mvc.perform(get("/api/docente/libros").header("X-Docente-Id", 15).param("grado", "7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El grado debe estar entre 1 y 6"));
        verifyNoInteractions(repositorio);
    }
}
