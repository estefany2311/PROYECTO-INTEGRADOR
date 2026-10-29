package com.proyecto.usuario.estudiante.exception;

// T-214: el nivel no existe o el docente lo desactivó
public class NivelNoEncontradoException extends RuntimeException {

    public NivelNoEncontradoException() {
        super("Ese nivel no existe");
    }
}
