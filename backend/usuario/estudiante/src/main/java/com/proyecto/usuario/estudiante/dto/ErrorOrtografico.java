package com.proyecto.usuario.estudiante.dto;

// inicio y fin son posiciones en el texto: la app subraya ese tramo (T-331)
public record ErrorOrtografico(String palabra, int inicio, int fin, String tipo, String sugerencia) {

    public static final String ORTOGRAFIA = "ortografia";
    public static final String TILDE = "tilde";
    public static final String MAYUSCULA = "mayuscula";
}
