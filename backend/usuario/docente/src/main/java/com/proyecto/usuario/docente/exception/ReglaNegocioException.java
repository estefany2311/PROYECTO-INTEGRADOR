package com.proyecto.usuario.docente.exception;

import org.springframework.http.HttpStatus;

// Clase para lanzar excepciones personalizadas de negocio con un estado HTTP específico
public class ReglaNegocioException extends RuntimeException {
    private final HttpStatus estado; // Variable para almacenar el código de estado (ej: BAD_REQUEST, CONFLICT)

    // Constructor que recibe el mensaje y el estado HTTP
    public ReglaNegocioException(String mensaje, HttpStatus estado) {
        super(mensaje);
        this.estado = estado;
    }

    // Getter para obtener el estado HTTP
    public HttpStatus getEstado() {
        return estado;
    }
}