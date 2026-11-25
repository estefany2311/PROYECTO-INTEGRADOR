package com.proyecto.usuario.estudiante.dto;

import java.util.List;

// inicio y fin son posiciones en el texto: la app subraya ese tramo (T-331)
// US-20 (T-364): sugerencias = hasta 3 palabras para corregir, de la más parecida a la menos parecida.
// "sugerencia" se mantiene para no romper la app: es la primera de la lista, o null si no hay ninguna.
public record ErrorOrtografico(String palabra, int inicio, int fin, String tipo, String sugerencia,
                               List<String> sugerencias) {

    public static final String ORTOGRAFIA = "ortografia";
    public static final String TILDE = "tilde";
    public static final String MAYUSCULA = "mayuscula";

    public ErrorOrtografico {
        sugerencias = sugerencias == null ? List.of() : List.copyOf(sugerencias);
    }

    // Con una sola sugerencia (o ninguna): la lista tiene solo esa
    public ErrorOrtografico(String palabra, int inicio, int fin, String tipo, String sugerencia) {
        this(palabra, inicio, fin, tipo, sugerencia, sugerencia == null ? List.of() : List.of(sugerencia));
    }

    // Con la lista: "sugerencia" es la primera
    public static ErrorOrtografico conSugerencias(String palabra, int inicio, int fin, String tipo,
                                                  List<String> sugerencias) {
        return new ErrorOrtografico(palabra, inicio, fin, tipo,
                sugerencias.isEmpty() ? null : sugerencias.get(0), sugerencias);
    }
}