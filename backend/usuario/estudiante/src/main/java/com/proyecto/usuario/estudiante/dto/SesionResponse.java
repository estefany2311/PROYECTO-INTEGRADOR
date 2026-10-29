package com.proyecto.usuario.estudiante.dto;

public record SesionResponse(String token, long estudianteId, String nombre, String avatarUrl) {}