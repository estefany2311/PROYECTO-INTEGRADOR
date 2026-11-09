package com.proyecto.usuario.estudiante.model;

// T-320 (US-17): fila de consigna_nivel (V4): lo que el alumno escribe en un nivel de escritura
public record Consigna(
        int nivelId,
        String consigna,
        String pista,
        String ilustracion,
        int palabrasMinimas
) {}
