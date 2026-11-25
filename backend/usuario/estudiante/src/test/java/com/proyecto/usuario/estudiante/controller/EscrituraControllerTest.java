package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.exception.EvidenciaNoEncontradaException;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.CompletarEscrituraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// T-435 (US-21): POST /api/estudiante/evidencias/{id}/completar
class EscrituraControllerTest {

    private static final String TOKEN = "Bearer token-de-ana";

    private CompletarEscrituraService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        AutenticadorTablet autenticador = mock(AutenticadorTablet.class);
        service = mock(CompletarEscrituraService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EscrituraController(autenticador, service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
        when(autenticador.estudianteDe(TOKEN)).thenReturn(7L);
    }

    @Test
    void devuelveElNivelDesbloqueado() throws Exception {
        when(service.completar(7L, 80L)).thenReturn(
                new CompletarNivelResponse(2, "completado", 17, 17, 2, true, 1, 3, "Lee y cuenta", true));

        mvc.perform(post("/api/estudiante/evidencias/80/completar").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estrellas").value(2))
                .andExpect(jsonPath("$.siguienteNivelNombre").value("Lee y cuenta"))
                .andExpect(jsonPath("$.siguienteRecienDesbloqueado").value(true));
    }

    @Test
    void elTextoDeOtroAlumnoResponde404() throws Exception {
        when(service.completar(7L, 81L)).thenThrow(new EvidenciaNoEncontradaException());

        mvc.perform(post("/api/estudiante/evidencias/81/completar").header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }
}
