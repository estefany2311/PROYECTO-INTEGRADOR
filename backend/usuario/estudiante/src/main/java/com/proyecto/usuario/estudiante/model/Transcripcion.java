package com.proyecto.usuario.estudiante.model;

import java.util.List;

// T-415: lo que devuelve el servicio de voz (backend/ia/voz)
public record Transcripcion(
        String texto,
        String idioma,
        double duracion,
        List<Palabra> palabras,
        String modelo
) {
    // Cada palabra con su segundo de inicio y fin; T-422 las alinea con el texto de la lectura
    public record Palabra(String palabra, double inicio, double fin, double probabilidad) {}
}
