package com.proyecto.usuario.estudiante.integracion;

import com.proyecto.usuario.estudiante.controller.AudioController;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.event.AudioRecibido;
import com.proyecto.usuario.estudiante.exception.ManejadorErrores;
import com.proyecto.usuario.estudiante.exception.ServicioVozNoDisponibleException;
import com.proyecto.usuario.estudiante.model.Lectura;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import com.proyecto.usuario.estudiante.repository.LecturaRepository;
import com.proyecto.usuario.estudiante.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// T-420 (US-13): de punta a punta con los audios de ejemplo de src/test/resources/audios.
// Validador, almacén, servicio y procesador son los reales; solo la base y Whisper son simulados.
class AudioIntegracionTest {

    private static final String TOKEN = "Bearer token-de-ana";
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final Transcripcion ESCUCHADO = new Transcripcion(
            "Había una vez un cóndor", "es", 3.0,
            List.of(new Transcripcion.Palabra("Había", 0.0, 0.4, 0.95)), "small");

    @TempDir Path carpeta;

    private EvidenciaRepository evidencias;
    private ClienteVoz voz;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        AutenticadorTablet autenticador = mock(AutenticadorTablet.class);
        when(autenticador.estudianteDe(TOKEN)).thenReturn(7L);
        NivelService niveles = mock(NivelService.class);
        when(niveles.detalle(7L, 1)).thenReturn(new NivelDetalleResponse(1, 1, "Primeros pasos", "lectura", "Lee en voz alta", "desbloqueado", 0));
        LecturaRepository lecturas = mock(LecturaRepository.class);
        when(lecturas.porId(5L)).thenReturn(Optional.of(new Lectura(5, "El cóndor", "Había una vez un cóndor.", "lectura", 2, "basica")));
        evidencias = mock(EvidenciaRepository.class);
        when(evidencias.crearAudio(eq(7L), eq(1), any(), any())).thenReturn(50L);
        voz = mock(ClienteVoz.class);

        AlmacenAudios almacen = new AlmacenAudios(carpeta.toString(), RELOJ);
        ProcesadorLecturas procesador = new ProcesadorLecturas(almacen, voz, evidencias, new SyncTaskExecutor(), RELOJ);
        // En la app real el evento llega después del commit; aquí se atiende en el momento
        AudioService servicio = new AudioService(niveles, lecturas, new ValidadorWav(), almacen, evidencias,
                evento -> procesador.alRecibir((AudioRecibido) evento), RELOJ);

        mvc = MockMvcBuilders.standaloneSetup(new AudioController(autenticador, servicio))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    private ResultActions subir(String audioDeEjemplo) throws Exception {
        byte[] contenido;
        try (InputStream entrada = getClass().getResourceAsStream("/audios/" + audioDeEjemplo)) {
            contenido = entrada.readAllBytes();
        }
        return mvc.perform(multipart("/api/estudiante/niveles/1/audio")
                .file(new MockMultipartFile("audio", audioDeEjemplo, "audio/wav", contenido))
                .param("lecturaId", "5")
                .header("Authorization", TOKEN));
    }

    private long audiosGuardados() throws IOException {
        if (!Files.exists(carpeta)) return 0;
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            return archivos.filter(Files::isRegularFile).count();
        }
    }

    @Test
    void lecturaValidaSeGuardaYSeTranscribe() throws Exception {
        when(voz.transcribir(any())).thenReturn(ESCUCHADO);

        subir("lectura_valida_3s.wav")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidenciaId").value(50))
                .andExpect(jsonPath("$.duracionMs").value(3000))
                .andExpect(jsonPath("$.estado").value("procesando"));

        assertEquals(1, audiosGuardados());
        verify(evidencias).registrarLectura(eq(50L), eq(5L), eq("Había una vez un cóndor."), eq(3000L), anyInt(), any());
        verify(evidencias).guardarTranscripcion(eq(50L), eq(ESCUCHADO), any());
    }

    @Test
    void cabeceraSinTamanoSeMideConLoQueLlego() throws Exception {
        when(voz.transcribir(any())).thenReturn(ESCUCHADO);

        subir("cabecera_sin_tamano.wav")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracionMs").value(2000));
    }

    @Test
    void siWhisperEstaApagadoLaEvidenciaQuedaEnError() throws Exception {
        when(voz.transcribir(any())).thenThrow(new ServicioVozNoDisponibleException("apagado", null));

        subir("lectura_valida_3s.wav").andExpect(status().isCreated());

        verify(evidencias).marcarError(eq(50L), eq("El servicio de voz no respondió"), any());
        assertEquals(1, audiosGuardados());   // el audio queda para que el docente lo escuche
    }

    @Test
    void grabacionMuyCortaNoSeGuarda() throws Exception {
        subir("muy_corta.wav")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La grabación es muy corta. Intenta leer otra vez"));

        assertEquals(0, audiosGuardados());
        verifyNoInteractions(voz);
    }

    @Test
    void estereoDe44kHzResponde415() throws Exception {
        subir("estereo_44khz.wav").andExpect(status().isUnsupportedMediaType());

        assertEquals(0, audiosGuardados());
    }

    @Test
    void ochoBitsResponde415() throws Exception {
        subir("ocho_bits.wav").andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void unTextoConExtensionWavResponde415() throws Exception {
        subir("no_es_audio.wav").andExpect(status().isUnsupportedMediaType());

        verify(evidencias, never()).crearAudio(anyLong(), anyInt(), any(), any());
    }
}
