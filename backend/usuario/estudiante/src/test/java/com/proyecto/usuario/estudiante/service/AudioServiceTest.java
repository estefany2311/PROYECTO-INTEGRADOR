package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.AudioRecibidoResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.AudioInvalidoException;
import com.proyecto.usuario.estudiante.exception.LecturaNoEncontradaException;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.model.Lectura;
import com.proyecto.usuario.estudiante.repository.LecturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.proyecto.usuario.estudiante.service.AudiosDePrueba.wav;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

// T-413 (US-13): qué pasa cuando la tablet sube la grabación de una lectura
@ExtendWith(MockitoExtension.class)
class AudioServiceTest {

    private static final Lectura LECTURA = new Lectura(3, "El cóndor y el zorro", "Había una vez un cóndor.", "lectura", 2, "basica");

    @Mock NivelService niveles;
    @Mock LecturaRepository lecturas;

    private AudioService service;

    @BeforeEach
    void preparar() {
        service = new AudioService(niveles, lecturas, new ValidadorWav());
    }

    private static NivelDetalleResponse nivel(String tipo) {
        return new NivelDetalleResponse(1, 1, "Primeros pasos", tipo, "Lee en voz alta", "desbloqueado", 0);
    }

    @Test
    void audioValidoDevuelveSuDuracion() {
        when(niveles.detalle(7L, 1)).thenReturn(nivel("lectura"));
        when(lecturas.porId(3L)).thenReturn(Optional.of(LECTURA));

        AudioRecibidoResponse respuesta = service.recibir(7L, 1, 3L, wav(4_000));

        assertEquals(new AudioRecibidoResponse(null, 1, 3L, 4_000, AudioService.VALIDADO), respuesta);
    }

    @Test
    void nivelBloqueadoResponde403() {
        when(niveles.detalle(7L, 2)).thenThrow(new NivelBloqueadoException("Primeros pasos"));

        assertThrows(NivelBloqueadoException.class, () -> service.recibir(7L, 2, 3L, wav(4_000)));
    }

    @Test
    void nivelDeEscrituraNoRecibeAudio() {
        when(niveles.detalle(7L, 2)).thenReturn(nivel("escritura"));

        assertThrows(AudioInvalidoException.class, () -> service.recibir(7L, 2, 3L, wav(4_000)));
    }

    @Test
    void lecturaQueNoExisteResponde404() {
        when(niveles.detalle(7L, 1)).thenReturn(nivel("lectura"));
        when(lecturas.porId(99L)).thenReturn(Optional.empty());

        assertThrows(LecturaNoEncontradaException.class, () -> service.recibir(7L, 1, 99L, wav(4_000)));
    }

    @Test
    void audioMuyCortoResponde400() {
        when(niveles.detalle(7L, 1)).thenReturn(nivel("lectura"));
        when(lecturas.porId(3L)).thenReturn(Optional.of(LECTURA));

        assertThrows(AudioInvalidoException.class, () -> service.recibir(7L, 1, 3L, wav(300)));
    }
}
