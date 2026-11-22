package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.ErrorOrtografico;
import com.proyecto.usuario.estudiante.dto.VerificarTextoResponse;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.exception.SesionVencidaException;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.OrtografiaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// T-328: POST /api/estudiante/ortografia/verificar
class OrtografiaControllerTest {

    private static final String TOKEN = "Bearer token-de-ana";

    private AutenticadorTablet autenticador;
    private OrtografiaService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        autenticador = mock(AutenticadorTablet.class);
        service = mock(OrtografiaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new OrtografiaController(autenticador, service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
        when(autenticador.estudianteDe(TOKEN)).thenReturn(1L);
    }

    @Test
    void devuelveLosErroresConSuPosicion() throws Exception {
        when(service.verificar(1L, "El arbol")).thenReturn(new VerificarTextoResponse(
                List.of(new ErrorOrtografico("arbol", 3, 8, "tilde", "árbol")), "media", false));

        mvc.perform(post("/api/estudiante/ortografia/verificar").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"El arbol\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errores[0].palabra").value("arbol"))
                .andExpect(jsonPath("$.errores[0].inicio").value(3))
                .andExpect(jsonPath("$.errores[0].fin").value(8))
                .andExpect(jsonPath("$.errores[0].tipo").value("tilde"))
                .andExpect(jsonPath("$.errores[0].sugerencia").value("árbol"))
                .andExpect(jsonPath("$.sensibilidad").value("media"));
    }

    @Test
    void sinTextoResponde400() throws Exception {
        mvc.perform(post("/api/estudiante/ortografia/verificar").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Falta el texto"));
        verify(service, never()).verificar(anyLong(), anyString());
    }

    @Test
    void textoDemasiadoLargoResponde400() throws Exception {
        String largo = "a".repeat(3001);

        mvc.perform(post("/api/estudiante/ortografia/verificar").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"" + largo + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sesionVencidaResponde401() throws Exception {
        when(autenticador.estudianteDe("Bearer viejo")).thenThrow(new SesionVencidaException());

        mvc.perform(post("/api/estudiante/ortografia/verificar").header("Authorization", "Bearer viejo")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"hola\"}"))
                .andExpect(status().isUnauthorized());
    }
}
