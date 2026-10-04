package com.proyecto.usuario.docente.dto;

// Record para la respuesta HTTP: simplifica la información del grado mostrando solo su ID y nombre
public record GradoResponse(
        Short id,        // ID del grado (ejemplo: 1)
        String nombre    // Nombre del grado (ejemplo: "1° Primary")
) {}