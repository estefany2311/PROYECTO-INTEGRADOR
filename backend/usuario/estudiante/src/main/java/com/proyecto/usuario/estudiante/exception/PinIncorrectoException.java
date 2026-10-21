package com.proyecto.usuario.estudiante.exception;

public class PinIncorrectoException extends RuntimeException {

    public PinIncorrectoException() {
        super("El PIN no es correcto");
    }
}