package com.proyecto.usuario.estudiante.exception;

// T-214: el alumno todavía no puede abrir este nivel (le falta completar el anterior)
public class NivelBloqueadoException extends RuntimeException {

    private final String nivelAnterior;

    public NivelBloqueadoException(String nivelAnterior) {
        super("Este nivel todavía está cerrado");
        this.nivelAnterior = nivelAnterior;
    }

    // Nombre del nivel que hay que terminar antes (null si no hay)
    public String getNivelAnterior() {
        return nivelAnterior;
    }
}
