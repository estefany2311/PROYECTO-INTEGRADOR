package com.proyecto.usuario.estudiante.model;

// Datos del alumno que necesita la tablet. pinHash nunca sale en una respuesta.
public record Estudiante(long id, String nombre, String avatarUrl, String pinHash, boolean activo) {}