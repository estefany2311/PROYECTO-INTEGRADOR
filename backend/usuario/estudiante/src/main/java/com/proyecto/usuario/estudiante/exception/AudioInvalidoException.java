package com.proyecto.usuario.estudiante.exception;

// T-413 · 400: el audio llegó vacío, muy corto, muy largo o para un nivel que no es de lectura
public class AudioInvalidoException extends RuntimeException {

    public AudioInvalidoException(String mensaje) {
        super(mensaje);
    }
}
