package com.proyecto.usuario.estudiante.exception;

// T-413 · 404: el lecturaId que mandó la tablet no existe en el catálogo
public class LecturaNoEncontradaException extends RuntimeException {

    public LecturaNoEncontradaException() {
        super("No encontramos la lectura que grabaste");
    }
}
