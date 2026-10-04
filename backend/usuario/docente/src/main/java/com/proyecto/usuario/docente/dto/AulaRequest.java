package com.proyecto.usuario.docente.dto;

import java.util.List;

// Record para la petición HTTP (POST): recibe el nombre del aula y la lista de IDs de los grados asignados
public record AulaRequest(
        String nombre,          // Nombre del aula (ejemplo: "Aula Rural")
        List<Short> gradoIds    // Lista de IDs de los grados seleccionados (ejemplo: [1, 2, 3])
) {}