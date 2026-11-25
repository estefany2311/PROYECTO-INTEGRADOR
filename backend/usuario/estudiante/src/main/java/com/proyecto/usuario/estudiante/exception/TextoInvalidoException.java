package com.proyecto.usuario.estudiante.exception;

// T-434 · 400: el nivel no es de escritura o el texto no llega al mínimo de palabras
public class TextoInvalidoException extends RuntimeException {

    public TextoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
