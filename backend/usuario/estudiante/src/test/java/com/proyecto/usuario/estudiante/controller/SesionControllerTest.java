package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.SesionResponse;
import com.proyecto.usuario.estudiante.exception.CuentaBloqueadaException;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.exception.PinIncorrectoException;
import com.proyecto.usuario.estudiante.service.SesionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T-233 (US-03): forma exacta de las respuestas de POST /api/estudiante/sesion.
 * La tablet lee estos campos para el aviso de intentos (T-207) y el bloqueo:
 * si alguien cambia un nombre o un código HTTP, estas pruebas fallan.
 */
class SesionControllerTest {

    private static final String URL = "/api/estudiante/sesion";

    private SesionService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(SesionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SesionController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    private static String cuerpo(long estudianteId, String pin) {
        return "{\"estudianteId\": " + estudianteId + ", \"pin\": \"" + pin + "\"}";
    }

    @Test
    void pinCorrectoDevuelveElToken() throws Exception {
        when(service.iniciarSesion(1L, "1234"))
                .thenReturn(new SesionResponse("token-abc", 1L, "Ana", "https://avatar/ana.png"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(1, "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-abc"))
                .andExpect(jsonPath("$.estudianteId").value(1))
                .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void pinIncorrectoResponde401ConLosIntentosRestantes() throws Exception {
        when(service.iniciarSesion(1L, "0000")).thenThrow(new PinIncorrectoException(3));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(1, "0000")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("El PIN no es correcto"))
                .andExpect(jsonPath("$.intentosRestantes").value(3))
                .andExpect(jsonPath("$.segundosRestantes").doesNotExist());
    }

    @Test
    void cuentaBloqueadaResponde423ConElTiempoDeEspera() throws Exception {
        when(service.iniciarSesion(1L, "0000")).thenThrow(new CuentaBloqueadaException(120));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(1, "0000")))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.mensaje").value("Demasiados intentos. Espera un momento"))
                .andExpect(jsonPath("$.segundosRestantes").value(120))
                .andExpect(jsonPath("$.intentosRestantes").doesNotExist())
                .andExpect(header().string("Retry-After", "120"));
    }

    // La validación corta antes de llegar al servicio: no cuenta como intento fallido
    @Test
    void pinQueNoTieneCuatroNumerosResponde400SinGastarIntentos() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(1, "12")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El PIN debe tener 4 números"));

        verify(service, never()).iniciarSesion(anyLong(), anyString());
    }

    @Test
    void jsonMalFormadoResponde400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"estudianteId\": \"abc\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Datos inválidos"));
    }
}
