package com.proyecto.usuario.estudiante.exception;

// T-218: el nivel está abierto, pero todavía no hay una lectura para el grado del alumno
public class LecturaNoDisponibleException extends RuntimeException {

    public LecturaNoDisponibleException() {
        super("Este nivel todavía no tiene una lectura para ti");
    }
}
