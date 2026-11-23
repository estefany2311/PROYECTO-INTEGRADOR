package com.proyecto.usuario.estudiante.model;

// T-424: lo que se necesita para calificar una evidencia de lectura
public record DatosLectura(long estudianteId, int nivelId, long lecturaId, String textoLectura, long duracionMs) {}
