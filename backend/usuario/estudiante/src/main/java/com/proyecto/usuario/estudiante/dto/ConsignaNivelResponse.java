package com.proyecto.usuario.estudiante.dto;

public record ConsignaNivelResponse(
        int nivelId,
        String nombreNivel,
        String consigna,
        String pista,
        String ilustracion,
        int palabrasMinimas
) {}
