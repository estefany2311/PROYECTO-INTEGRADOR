package com.proyecto.usuario.docente.dto;

import java.util.List;

// Record para la respuesta HTTP: envía los datos completos del aula con sus grados anidados
public record AulaResponse(
        Long id,                       // ID autogenerado del aula
        String nombre,                 // Nombre asignado al aula
        String codigo,                 // Código para que el alumno ingrese en la tablet
        List<GradoResponse> grados     // Lista formateada de objetos GradoResponse
) {
    // Constructor anterior (sin código): protege el código que aún use 3 campos
    public AulaResponse(Long id, String nombre, List<GradoResponse> grados) {
        this(id, nombre, null, grados);
    }
}