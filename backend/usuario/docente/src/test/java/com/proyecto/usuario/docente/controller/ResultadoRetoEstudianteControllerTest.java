package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.ResultadoRetoResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.ResultadoRetoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// forma de la respuesta que consume la app del alumno al terminar una lectura
class ResultadoRetoEstudianteControllerTest {

    private ResultadoRetoService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(ResultadoRetoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ResultadoRetoEstudianteController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveLaNotaLosPuntosYLosTotales() throws Exception {
        when(service.registrar(5L, 30L))
                .thenReturn(new ResultadoRetoResponse(30L, "lectura", 85, 9, 2, 40, 7));

        mvc.perform(post("/api/estudiante/evidencias/30/lectura").requestAttr("estudianteId", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenciaId").value(30))
                .andExpect(jsonPath("$.tipoReto").value("lectura"))
                .andExpect(jsonPath("$.nota").value(85))
                .andExpect(jsonPath("$.puntos").value(9))
                .andExpect(jsonPath("$.estrellas").value(2))
                .andExpect(jsonPath("$.puntosTotales").value(40))
                .andExpect(jsonPath("$.estrellasTotales").value(7));
    }

    @Test
    void siSeEstaProcesandoResponde409() throws Exception {
        when(service.registrar(5L, 30L)).thenThrow(new ReglaNegocioException(
                "Tu lectura todavía se está procesando", HttpStatus.CONFLICT));

        mvc.perform(post("/api/estudiante/evidencias/30/lectura").requestAttr("estudianteId", 5L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Tu lectura todavía se está procesando"));
    }

    @Test
    void laEvidenciaAjenaDevuelve404() throws Exception {
        when(service.registrar(6L, 30L)).thenThrow(new ReglaNegocioException(
                "La evidencia no existe", HttpStatus.NOT_FOUND));

        mvc.perform(post("/api/estudiante/evidencias/30/lectura").requestAttr("estudianteId", 6L))
                .andExpect(status().isNotFound());
    }
}
