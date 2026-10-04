package com.proyecto.usuario.docente.dto;

import java.util.List;

// Record para la respuesta HTTP: envía los datos completos del aula con sus grados anidados
public record AulaResponse(
        Long id,                       // ID autogenerado del aula
        String nombre,                 // Nombre asignado al aula
        List<GradoResponse> grados     // Lista formateada de objetos GradoResponse
) {}