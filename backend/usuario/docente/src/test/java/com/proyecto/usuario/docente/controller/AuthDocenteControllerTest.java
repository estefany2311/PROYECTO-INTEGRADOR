package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.SesionDocenteResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.seguridad.SesionDocenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// login docente: forma de las respuestas que consume el portal
class AuthDocenteControllerTest {

    private SesionDocenteService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(SesionDocenteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthDocenteController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void iniciarDevuelveLosTokensYElDocente() throws Exception {
        when(service.iniciar("prueba", "clave-de-prueba")).thenReturn(new SesionDocenteResponse(
                "a", "r", new SesionDocenteResponse.Docente(15L, "prueba", "Docente De Prueba")));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "prueba", "password": "clave-de-prueba"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value("a"))
                .andExpect(jsonPath("$.refresh").value("r"))
                .andExpect(jsonPath("$.docente.id").value(15))
                .andExpect(jsonPath("$.docente.nombre").value("Docente De Prueba"));
    }

    @Test
    void sinContrasenaNoLlegaAlServicio() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "prueba", "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Escribe tu contraseña"));
        verifyNoInteractions(service);
    }

    @Test
    void unRechazoLlevaSuMensaje() throws Exception {
        when(service.iniciar("prueba", "x")).thenThrow(new ReglaNegocioException(
                "Usuario o contraseña incorrectos", HttpStatus.UNAUTHORIZED));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "prueba", "password": "x"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void renovarDevuelveElNuevoAcceso() throws Exception {
        when(service.renovar("r")).thenReturn("nuevo");

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh": "r"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value("nuevo"));
    }

    @Test
    void cerrarSesionResponde204() throws Exception {
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh": "r"}
                                """))
                .andExpect(status().isNoContent());
        verify(service).cerrar("r");
    }
}
