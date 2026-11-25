package com.proyecto.usuario.estudiante.exception;

// T-416 · 404: la evidencia no existe o es de otro alumno
public class EvidenciaNoEncontradaException extends RuntimeException {

    public EvidenciaNoEncontradaException() {
        super("No encontramos esa grabación");
    }
}
