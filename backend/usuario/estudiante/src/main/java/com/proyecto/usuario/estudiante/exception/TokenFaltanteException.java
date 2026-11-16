package com.proyecto.usuario.estudiante.exception;

// T-236: se pidió cerrar sesión sin la cabecera "Authorization: Bearer <token>"
public class TokenFaltanteException extends RuntimeException {

    public TokenFaltanteException() {
        super("Falta el token de la sesión");
    }
}
