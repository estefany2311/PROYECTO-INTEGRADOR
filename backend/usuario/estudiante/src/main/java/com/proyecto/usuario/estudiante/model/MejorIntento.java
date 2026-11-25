package com.proyecto.usuario.estudiante.model;

// T-432 (US-16): cómo le fue antes en el mismo nivel; precision y estrellas son null si es su primer intento
public record MejorIntento(int intentos, Integer precision, Integer estrellas) {}
