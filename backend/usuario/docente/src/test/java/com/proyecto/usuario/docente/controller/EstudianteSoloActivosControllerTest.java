package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.service.EstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// T-184: el parámetro ?soloActivos decide qué método del servicio se usa
class EstudianteSoloActivosControllerTest {

    private EstudianteService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(EstudianteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EstudianteController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void conSoloActivosTrue_usaListarActivos() throws Exception {
        when(service.listarActivos(1L, 10L)).thenReturn(
                List.of(new EstudianteResponse(5L, "Ana", "Quispe", (short) 1, "1.º", true)));

        mvc.perform(get("/api/docente/aulas/10/estudiantes?soloActivos=true")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombres").value("Ana"));

        verify(service).listarActivos(1L, 10L);
        verify(service, never()).listar(anyLong(), anyLong());
    }

    @Test
    void sinParametro_usaListarYDevuelveTodos() throws Exception {
        when(service.listar(1L, 10L)).thenReturn(List.of());

        mvc.perform(get("/api/docente/aulas/10/estudiantes").header("X-Docente-Id", 1))
                .andExpect(status().isOk());

        verify(service).listar(1L, 10L);
        verify(service, never()).listarActivos(anyLong(), anyLong());
    }
}