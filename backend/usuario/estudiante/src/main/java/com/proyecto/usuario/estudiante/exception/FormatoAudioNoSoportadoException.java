package com.proyecto.usuario.estudiante.exception;

// T-413 · 415: no es un WAV PCM mono de 16 kHz y 16 bits, que es lo que graba la tablet
public class FormatoAudioNoSoportadoException extends RuntimeException {

    public FormatoAudioNoSoportadoException(String detalle) {
        super("El audio debe ser WAV mono de 16 kHz y 16 bits (" + detalle + ")");
    }
}
