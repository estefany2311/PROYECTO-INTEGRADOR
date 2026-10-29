package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarAvatarRequest(
        @NotNull(message = "Debes elegir un avatar")
        Short avatarId
) {}