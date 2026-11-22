package com.proyecto.usuario.estudiante.exception;

// T-320: el nivel está abierto, pero no es de escritura o todavía no tiene consigna
public class ConsignaNoDisponibleException extends RuntimeException {

    public ConsignaNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public static ConsignaNoDisponibleException noEsDeEscritura() {
        return new ConsignaNoDisponibleException("Este nivel no es de escritura");
    }

    public static ConsignaNoDisponibleException sinConsigna() {
        return new ConsignaNoDisponibleException("Este nivel todavía no tiene una consigna para ti");
    }
}
