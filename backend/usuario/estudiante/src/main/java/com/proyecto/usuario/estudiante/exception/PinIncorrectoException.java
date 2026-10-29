package com.proyecto.usuario.estudiante.exception;

public class PinIncorrectoException extends RuntimeException {

    private final int intentosRestantes;

    public PinIncorrectoException(int intentosRestantes) {
        super("El PIN no es correcto");
        this.intentosRestantes = intentosRestantes;
    }

    public int getIntentosRestantes() {
        return intentosRestantes;
    }
}
