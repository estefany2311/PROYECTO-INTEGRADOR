package com.proyecto.usuario.estudiante.model;

// T-218: una lectura del catálogo. La tabla `lecturas` es del módulo Admin (Daniela, Django); aquí solo se lee.
public record Lectura(long id, String titulo, String texto, String tipoReto, int grado, String dificultad) {}
