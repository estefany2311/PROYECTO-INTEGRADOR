package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.EstadoEvidenciaResponse;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.exception.TokenFaltanteException;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// T-416 (US-13): GET /api/estudiante/evidencias/{id}/estado
class EvidenciaControllerTest {

    private static final String TOKEN = "Bearer token-de-ana";

    private AutenticadorTablet autenticador;
    private EvidenciaRepository evidencias;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        autenticador = mock(AutenticadorTablet.class);
        evidencias = mock(EvidenciaRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new EvidenciaController(autenticador, evidencias))
                .setControllerAdvice(new ManejadorErrores())
                .build();
        when(autenticador.estudianteDe(TOKEN)).thenReturn(7L);
    }

    @Test
    void mientrasProcesaDiceProcesando() throws Exception {
        when(evidencias.estado(50L, 7L)).thenReturn(Optional.of(new EstadoEvidenciaResponse(50L, "procesando", null, null)));

        mvc.perform(get("/api/estudiante/evidencias/50/estado").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("procesando"));
    }

    @Test
    void cuandoTerminaDevuelveLoQueEscucho() throws Exception {
        when(evidencias.estado(50L, 7L))
                .thenReturn(Optional.of(new EstadoEvidenciaResponse(50L, "listo", "Había una vez", null)));

        mvc.perform(get("/api/estudiante/evidencias/50/estado").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("listo"))
                .andExpect(jsonPath("$.transcripcion").value("Había una vez"));
    }

    @Test
    void laEvidenciaDeOtroAlumnoResponde404() throws Exception {
        when(evidencias.estado(51L, 7L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/estudiante/evidencias/51/estado").header("Authorization", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos esa grabación"));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        when(autenticador.estudianteDe(null)).thenThrow(new TokenFaltanteException());

        mvc.perform(get("/api/estudiante/evidencias/50/estado"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(evidencias);
    }
}
