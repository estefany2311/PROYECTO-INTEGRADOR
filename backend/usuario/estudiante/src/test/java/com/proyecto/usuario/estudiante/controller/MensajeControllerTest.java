package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.MensajeDocenteResponse;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.exception.TokenFaltanteException;
import com.proyecto.usuario.estudiante.repository.MensajeDocenteRepository;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// GET /api/estudiante/mensajes: comentarios del docente para el alumno del token
class MensajeControllerTest {

    private static final String TOKEN = "Bearer token-de-ana";

    private AutenticadorTablet autenticador;
    private MensajeDocenteRepository mensajes;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        autenticador = mock(AutenticadorTablet.class);
        mensajes = mock(MensajeDocenteRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new MensajeController(autenticador, mensajes))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveLosComentariosDelAlumnoDelToken() throws Exception {
        when(autenticador.estudianteDe(TOKEN)).thenReturn(7L);
        when(mensajes.delEstudiante(7L)).thenReturn(List.of(new MensajeDocenteResponse(
                80L, 1, "Mi primera lectura", "audio", "Buen trabajo, sigue practicando!",
                LocalDateTime.of(2026, 11, 26, 4, 30))));

        mvc.perform(get("/api/estudiante/mensajes").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nivelNombre").value("Mi primera lectura"))
                .andExpect(jsonPath("$[0].comentario").value("Buen trabajo, sigue practicando!"));
    }

    @Test
    void sinComentariosDevuelveListaVacia() throws Exception {
        when(autenticador.estudianteDe(TOKEN)).thenReturn(7L);
        when(mensajes.delEstudiante(7L)).thenReturn(List.of());

        mvc.perform(get("/api/estudiante/mensajes").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        when(autenticador.estudianteDe(null)).thenThrow(new TokenFaltanteException());

        mvc.perform(get("/api/estudiante/mensajes"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(mensajes);
    }
}
