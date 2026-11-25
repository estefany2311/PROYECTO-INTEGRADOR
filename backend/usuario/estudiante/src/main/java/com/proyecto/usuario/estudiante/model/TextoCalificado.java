package com.proyecto.usuario.estudiante.model;

import java.util.List;

// T-428: el texto de la lectura y el tipo de cada palabra calificada (sin las agregadas), en orden
public record TextoCalificado(String texto, List<String> tipos) {}
