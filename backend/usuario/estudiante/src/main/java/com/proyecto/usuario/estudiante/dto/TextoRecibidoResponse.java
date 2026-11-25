package com.proyecto.usuario.estudiante.dto;

import java.util.List;

// T-434 (US-21): la evidencia de texto creada; con evidenciaId la app pide el puntaje de escritura (US-22)
public record TextoRecibidoResponse(long evidenciaId, int nivelId, int palabras, List<String> palabrasMarcadas) {}
