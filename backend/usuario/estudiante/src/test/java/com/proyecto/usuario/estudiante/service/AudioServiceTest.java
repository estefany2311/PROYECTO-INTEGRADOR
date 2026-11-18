package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.AudioRecibidoResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.AudioInvalidoException;
import com.proyecto.usuario.estudiante.exception.LecturaNoEncontradaException;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.model.Lectura;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import com.proyecto.usuario.estudiante.repository.LecturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static com.proyecto.usuario.estudiante.service.AudiosDePrueba.wav;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// T-413/T-414 (US-13): qué pasa cuando la tablet sube la grabación de una lectura
@ExtendWith(MockitoExtension.class)
class AudioServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);
    private static final Lectura LECTURA = new Lectura(3, "El cóndor y el zorro", "Había una vez un cóndor.", "lectura", 2, "basica");

    @Mock NivelService niveles;
    @Mock LecturaRepository lecturas;
    @Mock AlmacenAudios almacen;
    @Mock EvidenciaRepository evidencias;

    private AudioService service;

    @BeforeEach
    void preparar() {
        service = new AudioService(niveles, lecturas, new ValidadorWav(), almacen, evidencias, RELOJ);
    }

    private static NivelDetalleResponse nivel(String tipo) {
        return new NivelDetalleResponse(1, 1, "Primeros pasos", tipo, "Lee en voz alta", "desbloqueado", 0);
    }

    @Test
    void guardaElAudioYCreaLaEvidenciaConElTextoLeido() {
        byte[] audio = wav(4_000);
        when(niveles.detalle(7L, 1)).thenReturn(nivel("lectura"));
        when(lecturas.porId(3L)).thenReturn(Optional.of(LECTURA));
        when(almacen.guardar(7L, 1, audio)).thenReturn("est_7/nivel_1.wav");
        when(evidencias.crearAudio(7L, 1, "est_7/nivel_1.wav", AHORA)).thenReturn(50L);

        AudioRecibidoResponse respuesta = service.recibir(7L, 1, 3L, audio);

        assertEquals(new AudioRecibidoResponse(50L, 1, 3L, 4_000, AudioService.RECIBIDO), respuesta);
        verify(evidencias).registrarLectura(50L, 3L, "Había una vez un cóndor.", 4_000, audio.length, AHORA);
        verify(almacen, never()).borrar(any());
    }

    @Test
    void siFallaLaBaseSeBorraElArchivo() {
        byte[] audio = wav(4_000);
        when(niveles.detalle(7L, 1)).thenReturn(nivel("lectura"));
        when(lecturas.porId(3L)).thenReturn(Optional.of(LECTURA));
        when(almacen.guardar(7L, 1, audio)).thenReturn("est_7/nivel_1.wav");
        when(evidencias.crearAudio(anyLong(), anyInt(), any(), any())).thenThrow(new IllegalStateException("sin base"));

        assertThrows(IllegalStateException.class, () -> service.recibir(7L, 1, 3L, audio));

        verify(almacen).borrar("est_7/nivel_1.wav");
    }

    @Test
    void nivelBloqueadoNoGuardaNada() {
        when(niveles.detalle(7L, 2)).thenThrow(new NivelBloqueadoException("Primeros pasos"));

        assertThrows(NivelBloqueadoException.class, () -> service.recibir(7L, 2, 3L, wav(4_000)));

        verifyNoInteractions(almacen, evidencias);
    }

    @Test
    void nivelDeEscrituraNoRecibeAudio() {
        when(niveles.detalle(7L, 2)).thenReturn(nivel("escritura"));

        assertThrows(AudioInvalidoException.class, () -> service.recibir(7L, 2, 3L, wav(4_000)));

        verifyNoInteractions(almacen, evidencias);
    }

    @Test
    void lecturaQueNoExisteResponde404() {
        when(niveles.detalle(7L, 1)).thenReturn(nivel("lectura"));
        when(lecturas.porId(99L)).thenReturn(Optional.empty());

        assertThrows(LecturaNoEncontradaException.class, () -> service.recibir(7L, 1, 99L, wav(4_000)));

        verifyNoInteractions(almacen, evidencias);
    }

    @Test
    void audioMuyCortoNoSeGuarda() {
        when(niveles.detalle(7L, 1)).thenReturn(nivel("lectura"));
        when(lecturas.porId(3L)).thenReturn(Optional.of(LECTURA));

        assertThrows(AudioInvalidoException.class, () -> service.recibir(7L, 1, 3L, wav(300)));

        verifyNoInteractions(almacen, evidencias);
    }
}
