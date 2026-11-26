package com.proyecto.usuario.docente.dto;

/** Login docente: los tokens de la sesión y los datos de la cuenta. */
public record SesionDocenteResponse(String access, String refresh, Docente docente) {

    public record Docente(Long id, String usuario, String nombre) {
    }
}
