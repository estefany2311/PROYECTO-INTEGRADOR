package com.proyecto.usuario.estudiante.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CodigoAulaInvalidoException extends RuntimeException {
    public CodigoAulaInvalidoException(String mensaje) {
        super(mensaje);
    }
}