package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AulaRequest(
        // Valida que el nombre no sea nulo ni esté vacío o lleno de espacios
        @NotBlank(message = "El nombre del aula es obligatorio")
        String nombre,

        // Valida que la lista contenga al menos un elemento de grado
        @NotEmpty(message = "Debes seleccionar al menos un grado")
        List<Short> gradoIds
) {}