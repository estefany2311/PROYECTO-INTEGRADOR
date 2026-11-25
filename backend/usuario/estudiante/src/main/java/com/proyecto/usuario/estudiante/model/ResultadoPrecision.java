package com.proyecto.usuario.estudiante.model;

// T-423: precisión de la lectura en voz alta (0 a 100) comparada con el umbral del grado (US-51)
public record ResultadoPrecision(
        int precision,      // % de palabras bien leídas dentro de la parte que alcanzó a leer
        int correctas,
        int evaluadas,      // palabras del texto que se califican
        int total,          // palabras del texto completo
        int cobertura,      // % del texto que alcanzó a leer
        int agregadas,
        int umbral,
        boolean aprobado
) {}
