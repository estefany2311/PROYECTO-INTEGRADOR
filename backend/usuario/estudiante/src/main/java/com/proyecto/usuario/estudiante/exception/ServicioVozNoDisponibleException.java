package com.proyecto.usuario.estudiante.exception;

// T-415 · 503: el servicio de voz a texto no respondió o respondió con error
public class ServicioVozNoDisponibleException extends RuntimeException {

    public ServicioVozNoDisponibleException(String detalle, Throwable causa) {
        super("El servicio de voz no está disponible: " + detalle, causa);
    }
}
