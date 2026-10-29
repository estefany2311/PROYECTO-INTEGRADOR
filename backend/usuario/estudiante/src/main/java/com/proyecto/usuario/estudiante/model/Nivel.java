package com.proyecto.usuario.estudiante.model;

// T-214: un nivel del mapa. La tabla `nivel` la crea el backend docente (V7, T-197); aquí solo se lee.
public record Nivel(int id, int orden, String nombre, String tipoReto, String descripcion) {}
