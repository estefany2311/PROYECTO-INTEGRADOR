package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.FluidezResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.FluidezService;
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

// T-398 (US-15): forma de la respuesta que consume la app del alumno
class FluidezEstudianteControllerTest {

    private FluidezService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(FluidezService.class);
        mvc = MockMvcBuilders.standaloneSetup(new FluidezEstudianteController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveElPuntajeYElDetalleDeLaFluidez() throws Exception {
        when(service.calcular(5L, 30L)).thenReturn(new FluidezResponse(
                30L, 73, 42, 60, 2, 1, "¡Vas muy bien! Intenta leer sin detenerte tanto entre palabras."));

        mvc.perform(get("/api/estudiante/evidencias/30/fluidez").requestAttr("estudianteId", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenciaId").value(30))
                .andExpect(jsonPath("$.puntajeFluidez").value(73))
                .andExpect(jsonPath("$.palabrasPorMinuto").value(42))
                .andExpect(jsonPath("$.palabrasEsperadas").value(60))
                .andExpect(jsonPath("$.pausasLargas").value(2))
                .andExpect(jsonPath("$.vacilaciones").value(1))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void siSeEstaProcesandoResponde409ConMensaje() throws Exception {
        when(service.calcular(5L, 30L)).thenThrow(new ReglaNegocioException(
                "Tu lectura todavía se está procesando", HttpStatus.CONFLICT));

        mvc.perform(get("/api/estudiante/evidencias/30/fluidez").requestAttr("estudianteId", 5L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Tu lectura todavía se está procesando"));
    }

    @Test
    void laEvidenciaAjenaDevuelve404() throws Exception {
        when(service.calcular(6L, 30L)).thenThrow(new ReglaNegocioException(
                "La evidencia no existe", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/estudiante/evidencias/30/fluidez").requestAttr("estudianteId", 6L))
                .andExpect(status().isNotFound());
    }
}
