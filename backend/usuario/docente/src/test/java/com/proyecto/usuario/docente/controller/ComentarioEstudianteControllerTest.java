package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.ComentarioEvidenciaResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.ComentarioEstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// T-387 (US-34): forma de la respuesta que consume la app del alumno
class ComentarioEstudianteControllerTest {

    private ComentarioEstudianteService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(ComentarioEstudianteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ComentarioEstudianteController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveElComentarioDelAlumnoAutenticado() throws Exception {
        when(service.obtenerComentario(1L, 30L))
                .thenReturn(new ComentarioEvidenciaResponse(30L, "Muy bien leído."));

        mvc.perform(get("/api/estudiante/evidencias/30/comentario")
                        .requestAttr("estudianteId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenciaId").value(30))
                .andExpect(jsonPath("$.comentario").value("Muy bien leído."));
    }

    @Test
    void evidenciaAjenaDevuelve404ConMensaje() throws Exception {
        when(service.obtenerComentario(1L, 99L))
                .thenThrow(new ReglaNegocioException("La evidencia no existe", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/estudiante/evidencias/99/comentario")
                        .requestAttr("estudianteId", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La evidencia no existe"));
    }
}
