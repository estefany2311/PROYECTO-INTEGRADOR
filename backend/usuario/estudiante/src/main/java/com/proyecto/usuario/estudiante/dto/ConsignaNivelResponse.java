package com.proyecto.usuario.estudiante.dto;

/**
 * T-320 (US-17): lo que muestra la pantalla de la consigna (T-321) antes de abrir el editor.
 * ilustracion: nombre del dibujo que pinta la app ("animal", "libro", "lapiz"...).
 * palabrasMinimas: lo usa el contador de palabras del editor (T-324).
 */
public record ConsignaNivelResponse(
        int nivelId,
        String nombreNivel,
        String consigna,
        String pista,
        String ilustracion,
        int palabrasMinimas
) {}
