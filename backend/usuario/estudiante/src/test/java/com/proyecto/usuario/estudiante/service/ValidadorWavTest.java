package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.exception.AudioInvalidoException;
import com.proyecto.usuario.estudiante.exception.FormatoAudioNoSoportadoException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.proyecto.usuario.estudiante.service.AudiosDePrueba.wav;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// T-413 (US-13): solo se acepta el WAV que graba la tablet, entre 1 segundo y 2 minutos
class ValidadorWavTest {

    private final ValidadorWav validador = new ValidadorWav();

    @Test
    void wavDeLaTabletDevuelveSuDuracion() {
        assertEquals(2_500, validador.validar(wav(2_500)));
    }

    @Test
    void dosMinutosJustosSeAceptan() {
        assertEquals(120_000, validador.validar(wav(120_000)));
    }

    @Test
    void sinAudioResponde400() {
        assertThrows(AudioInvalidoException.class, () -> validador.validar(new byte[0]));
        assertThrows(AudioInvalidoException.class, () -> validador.validar(null));
    }

    @Test
    void menosDeUnSegundoEsMuyCorto() {
        AudioInvalidoException e = assertThrows(AudioInvalidoException.class, () -> validador.validar(wav(500)));
        assertTrue(e.getMessage().contains("muy corta"));
    }

    @Test
    void masDeDosMinutosYMargenSeRechaza() {
        assertThrows(AudioInvalidoException.class, () -> validador.validar(wav(130_000)));
    }

    @Test
    void unArchivoQueNoEsWavResponde415() {
        byte[] mp3 = "ID3 esto no es un wav, es otra cosa cualquiera de prueba".getBytes();
        assertThrows(FormatoAudioNoSoportadoException.class, () -> validador.validar(mp3));
    }

    @Test
    void estereoNoSeAcepta() {
        assertThrows(FormatoAudioNoSoportadoException.class, () -> validador.validar(wav(2_000, 2, 16_000, 16)));
    }

    @Test
    void otraFrecuenciaNoSeAcepta() {
        assertThrows(FormatoAudioNoSoportadoException.class, () -> validador.validar(wav(2_000, 1, 44_100, 16)));
    }

    @Test
    void ochoBitsNoSeAcepta() {
        assertThrows(FormatoAudioNoSoportadoException.class, () -> validador.validar(wav(2_000, 1, 16_000, 8)));
    }

    // Si la app se cierra a la mitad, la cabecera puede quedar con tamaño 0: se mide lo que llegó
    @Test
    void cabeceraConTamanoEnCeroUsaLoQueRealmenteLlego() {
        byte[] audio = wav(3_000);
        ByteBuffer.wrap(audio).order(ByteOrder.LITTLE_ENDIAN).putInt(40, 0);

        assertEquals(3_000, validador.validar(audio));
    }
}
