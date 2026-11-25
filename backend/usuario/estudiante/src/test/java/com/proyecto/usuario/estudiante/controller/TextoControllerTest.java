package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.TextoRecibidoResponse;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.exception.TextoInvalidoException;
import com.proyecto.usuario.estudiante.exception.TokenFaltanteException;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import com.proyecto.usuario.estudiante.service.TextoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// T-434 (US-21): POST /api/estudiante/niveles/{id}/texto
class TextoControllerTest {

    private static final String TOKEN = "Bearer token-de-ana";

    private AutenticadorTablet autenticador;
    private TextoService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        autenticador = mock(AutenticadorTablet.class);
        service = mock(TextoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new TextoController(autenticador, service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
        when(autenticador.estudianteDe(TOKEN)).thenReturn(7L);
    }

    @Test
    void textoValidoResponde201ConLaEvidencia() throws Exception {
        when(service.recibir(7L, 2, "Mi perro es jugeton."))
                .thenReturn(new TextoRecibidoResponse(80L, 2, 4, List.of("jugeton")));

        mvc.perform(post("/api/estudiante/niveles/2/texto").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Mi perro es jugeton.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidenciaId").value(80))
                .andExpect(jsonPath("$.palabras").value(4))
                .andExpect(jsonPath("$.palabrasMarcadas[0]").value("jugeton"));
    }

    @Test
    void textoVacioResponde400SinLlamarAlServicio() throws Exception {
        mvc.perform(post("/api/estudiante/niveles/2/texto").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void siLeFaltanPalabrasResponde400ConElMensaje() throws Exception {
        when(service.recibir(eq(7L), eq(2), anyString()))
                .thenThrow(new TextoInvalidoException("Te faltan 4 palabras para enviar tu texto"));

        mvc.perform(post("/api/estudiante/niveles/2/texto").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Hola amigo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Te faltan 4 palabras para enviar tu texto"));
    }

    @Test
    void unNivelBloqueadoResponde403() throws Exception {
        when(service.recibir(eq(7L), eq(5), anyString())).thenThrow(new NivelBloqueadoException("Palabras nuevas"));

        mvc.perform(post("/api/estudiante/niveles/5/texto").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Hola amigo\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        when(autenticador.estudianteDe(null)).thenThrow(new TokenFaltanteException());

        mvc.perform(post("/api/estudiante/niveles/2/texto")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Hola amigo\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
