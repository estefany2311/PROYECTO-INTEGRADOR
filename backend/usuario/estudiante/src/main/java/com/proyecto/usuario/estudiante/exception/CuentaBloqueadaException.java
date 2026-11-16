package com.proyecto.usuario.estudiante.exception;

public class CuentaBloqueadaException extends RuntimeException {

    private final long segundosRestantes;

    public CuentaBloqueadaException(long segundosRestantes) {
        super("Demasiados intentos. Espera un momento");
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}
