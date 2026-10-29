package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.exception.NivelNoEncontradoException;
import com.proyecto.usuario.estudiante.exception.SesionVencidaException;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.NivelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T-243 (US-07): forma de las respuestas de GET /api/estudiante/niveles/{id}
 * que lee la tablet (NivelRepository de la app, T-215).
 */
class NivelControllerTest {

    private static final String TOKEN = "Bearer token-de-ana";

    private AutenticadorTablet autenticador;
    private NivelService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        autenticador = mock(AutenticadorTablet.class);
        service = mock(NivelService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NivelController(autenticador, service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
        when(autenticador.estudianteDe(TOKEN)).thenReturn(1L);
    }

    @Test
    void nivelDisponibleDevuelveSuTipoDeReto() throws Exception {
        when(service.detalle(1L, 1)).thenReturn(new NivelDetalleResponse(
                1, 1, "Primeros pasos", "lectura", "Lee palabras y oraciones cortas", "desbloqueado", 0));

        mvc.perform(get("/api/estudiante/niveles/1").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Primeros pasos"))
                .andExpect(jsonPath("$.tipoReto").value("lectura"))
                .andExpect(jsonPath("$.estado").value("desbloqueado"));
    }

    // Un nivel bloqueado no se abre: 403 con el nivel que hay que terminar antes
    @Test
    void nivelBloqueadoResponde403ConElNivelAnterior() throws Exception {
        when(service.detalle(1L, 2)).thenThrow(new NivelBloqueadoException("Primeros pasos"));

        mvc.perform(get("/api/estudiante/niveles/2").header("Authorization", TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Este nivel todavía está cerrado"))
                .andExpect(jsonPath("$.nivelAnterior").value("Primeros pasos"))
                .andExpect(jsonPath("$.nombre").doesNotExist());
    }

    @Test
    void nivelQueNoExisteResponde404() throws Exception {
        when(service.detalle(1L, 99)).thenThrow(new NivelNoEncontradoException());

        mvc.perform(get("/api/estudiante/niveles/99").header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinTokenResponde401YNoBuscaElNivel() throws Exception {
        when(autenticador.estudianteDe(null)).thenCallRealMethod();

        mvc.perform(get("/api/estudiante/niveles/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Falta el token de la sesión"));

        verify(service, never()).detalle(anyLong(), anyInt());
    }

    @Test
    void sesionVencidaResponde401() throws Exception {
        when(autenticador.estudianteDe("Bearer viejo")).thenThrow(new SesionVencidaException());

        mvc.perform(get("/api/estudiante/niveles/1").header("Authorization", "Bearer viejo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Tu sesión terminó. Vuelve a escribir tu PIN"));

        verify(service, never()).detalle(anyLong(), anyInt());
    }
}
