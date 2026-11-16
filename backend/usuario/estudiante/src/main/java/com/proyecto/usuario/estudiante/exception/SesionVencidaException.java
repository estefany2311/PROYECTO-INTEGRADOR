package com.proyecto.usuario.estudiante.exception;

// T-214: el token no existe, venció o se cerró con "Salir" (T-236)
public class SesionVencidaException extends RuntimeException {

    public SesionVencidaException() {
        super("Tu sesión terminó. Vuelve a escribir tu PIN");
    }
}
