package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.EstadoEvidenciaResponse;
import com.proyecto.usuario.estudiante.dto.ResultadoLecturaResponse;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.exception.TokenFaltanteException;
import com.proyecto.usuario.estudiante.model.PalabraTexto;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.ResultadoLecturaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// T-416 (US-13): GET .../estado · T-425/T-428 (US-14): GET .../resultado
class EvidenciaControllerTest {

    private static final String TOKEN = "Bearer token-de-ana";

    private AutenticadorTablet autenticador;
    private EvidenciaRepository evidencias;
    private ResultadoLecturaService resultados;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        autenticador = mock(AutenticadorTablet.class);
        evidencias = mock(EvidenciaRepository.class);
        resultados = mock(ResultadoLecturaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EvidenciaController(autenticador, evidencias, resultados))
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

    // T-425
    @Test
    void elResultadoTraeEstrellasPuntosYNivelDesbloqueado() throws Exception {
        when(resultados.resultado(50L, 7L)).thenReturn(Optional.of(new ResultadoLecturaResponse(
                50L, 1, "listo", "Había una vez", 94, 15, 16, 100, 70, true, 3, 9, 2, "Palabras nuevas", 33L, List.of())));

        mvc.perform(get("/api/estudiante/evidencias/50/resultado").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precision").value(94))
                .andExpect(jsonPath("$.estrellas").value(3))
                .andExpect(jsonPath("$.puntos").value(9))
                .andExpect(jsonPath("$.nivelDesbloqueadoNombre").value("Palabras nuevas"))
                .andExpect(jsonPath("$.asignacionId").value(33));
    }

    @Test
    void elResultadoDeOtroAlumnoResponde404() throws Exception {
        when(resultados.resultado(51L, 7L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/estudiante/evidencias/51/resultado").header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }

    // T-428
    @Test
    void elResultadoTraeLasPalabrasPorMejorar() throws Exception {
        when(resultados.resultado(50L, 7L)).thenReturn(Optional.of(new ResultadoLecturaResponse(
                50L, 1, "listo", "Había una vez", 67, 2, 3, 100, 70, false, 1, 5, null, null, null,
                List.of(new PalabraTexto("Había", false), new PalabraTexto("una", true), new PalabraTexto("vez.", false)))));

        mvc.perform(get("/api/estudiante/evidencias/50/resultado").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.palabras.length()").value(3))
                .andExpect(jsonPath("$.palabras[1].texto").value("una"))
                .andExpect(jsonPath("$.palabras[1].mejorar").value(true))
                .andExpect(jsonPath("$.palabras[2].mejorar").value(false));
    }
}
