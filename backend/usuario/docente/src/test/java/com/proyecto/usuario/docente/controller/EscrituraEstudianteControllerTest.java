package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.ResultadoEscrituraResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.EscrituraService;
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

// T-404 (US-22): forma de la respuesta que consume la pantalla de resultado de escritura
class EscrituraEstudianteControllerTest {

    private EscrituraService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(EscrituraService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EscrituraEstudianteController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveElPuntajeElDetalleYLasEstrellas() throws Exception {
        when(service.evaluar(5L, 31L)).thenReturn(new ResultadoEscrituraResponse(
                31L, 78, 80, 83, 67, 10, 12, 1, 3, 2, 1,
                "¡Muy bien! Tu escritura va por buen camino. Recuerda empezar cada oración con mayúscula y terminarla con punto."));

        mvc.perform(post("/api/estudiante/evidencias/31/escritura").requestAttr("estudianteId", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenciaId").value(31))
                .andExpect(jsonPath("$.puntajeEscritura").value(78))
                .andExpect(jsonPath("$.puntajeOrtografia").value(80))
                .andExpect(jsonPath("$.puntajeLongitud").value(83))
                .andExpect(jsonPath("$.puntajeConsigna").value(67))
                .andExpect(jsonPath("$.palabras").value(10))
                .andExpect(jsonPath("$.palabrasMinimas").value(12))
                .andExpect(jsonPath("$.errores").value(1))
                .andExpect(jsonPath("$.estrellas").value(1))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void laEvidenciaAjenaDevuelve404() throws Exception {
        when(service.evaluar(6L, 31L)).thenThrow(new ReglaNegocioException(
                "La evidencia no existe", HttpStatus.NOT_FOUND));

        mvc.perform(post("/api/estudiante/evidencias/31/escritura").requestAttr("estudianteId", 6L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La evidencia no existe"));
    }

    @Test
    void unaEvidenciaDeAudioDevuelve400() throws Exception {
        when(service.evaluar(5L, 30L)).thenThrow(new ReglaNegocioException(
                "Solo los textos escritos tienen puntaje de escritura", HttpStatus.BAD_REQUEST));

        mvc.perform(post("/api/estudiante/evidencias/30/escritura").requestAttr("estudianteId", 5L))
                .andExpect(status().isBadRequest());
    }
}
