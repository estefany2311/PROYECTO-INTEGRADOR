package com.proyecto.usuario.estudiante.dto;

import java.util.List;

public record VerificarTextoResponse(List<ErrorOrtografico> errores, String sensibilidad, boolean ignorarTildes) {}
