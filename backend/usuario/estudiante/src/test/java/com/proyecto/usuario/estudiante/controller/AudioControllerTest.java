package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.AudioRecibidoResponse;
import com.proyecto.usuario.estudiante.exception.AudioInvalidoException;
import com.proyecto.usuario.estudiante.exception.FormatoAudioNoSoportadoException;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.exception.TokenFaltanteException;
import com.proyecto.usuario.estudiante.service.AudioService;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static com.proyecto.usuario.estudiante.service.AudiosDePrueba.wav;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// T-413 (US-13): POST /api/estudiante/niveles/{id}/audio
class AudioControllerTest {

    private static final String TOKEN = "Bearer token-de-ana";

    private AutenticadorTablet autenticador;
    private AudioService service;
    private MockMvc mvc;
    private final MockMultipartFile audio = new MockMultipartFile("audio", "nivel_1.wav", "audio/wav", wav(3_000));

    @BeforeEach
    void preparar() {
        autenticador = mock(AutenticadorTablet.class);
        service = mock(AudioService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AudioController(autenticador, service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
        when(autenticador.estudianteDe(TOKEN)).thenReturn(7L);
    }

    @Test
    void audioValidoResponde200ConSuDuracion() throws Exception {
        when(service.recibir(eq(7L), eq(1), eq(3L), any()))
                .thenReturn(new AudioRecibidoResponse(null, 1, 3L, 3_000, AudioService.VALIDADO));

        mvc.perform(multipart("/api/estudiante/niveles/1/audio").file(audio).param("lecturaId", "3").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracionMs").value(3000))
                .andExpect(jsonPath("$.estado").value("validado"));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        when(autenticador.estudianteDe(null)).thenThrow(new TokenFaltanteException());

        mvc.perform(multipart("/api/estudiante/niveles/1/audio").file(audio).param("lecturaId", "3"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void sinAudioResponde400() throws Exception {
        mvc.perform(multipart("/api/estudiante/niveles/1/audio").param("lecturaId", "3").header("Authorization", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Falta el audio"));
    }

    @Test
    void sinLecturaIdResponde400() throws Exception {
        mvc.perform(multipart("/api/estudiante/niveles/1/audio").file(audio).header("Authorization", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Falta el parámetro lecturaId"));
    }

    @Test
    void formatoEquivocadoResponde415() throws Exception {
        when(service.recibir(anyLong(), anyInt(), anyLong(), any()))
                .thenThrow(new FormatoAudioNoSoportadoException("no es mono"));

        mvc.perform(multipart("/api/estudiante/niveles/1/audio").file(audio).param("lecturaId", "3").header("Authorization", TOKEN))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void audioMuyCortoResponde400ConElMensaje() throws Exception {
        when(service.recibir(anyLong(), anyInt(), anyLong(), any()))
                .thenThrow(new AudioInvalidoException("La grabación es muy corta. Intenta leer otra vez"));

        mvc.perform(multipart("/api/estudiante/niveles/1/audio").file(audio).param("lecturaId", "3").header("Authorization", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La grabación es muy corta. Intenta leer otra vez"));
    }

    @Test
    void nivelBloqueadoResponde403() throws Exception {
        when(service.recibir(anyLong(), anyInt(), anyLong(), any())).thenThrow(new NivelBloqueadoException("Primeros pasos"));

        mvc.perform(multipart("/api/estudiante/niveles/2/audio").file(audio).param("lecturaId", "3").header("Authorization", TOKEN))
                .andExpect(status().isForbidden());
    }

    // El límite de tamaño lo aplica Spring antes de llegar al controlador
    @Test
    void archivoDemasiadoGrandeResponde413() {
        var respuesta = new ManejadorErrores().audioMuyGrande(new MaxUploadSizeExceededException(5 * 1024 * 1024));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, respuesta.getStatusCode());
    }
}
