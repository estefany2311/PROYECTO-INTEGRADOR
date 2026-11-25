package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * T-195 (US-37): lectura que se asigna a todos los alumnos del grupo.
 *
 * <p>T-388 (US-38): {@code fechaLimite} es opcional y se aplica a todas las
 * tareas creadas; no puede estar en el pasado.</p>
 */
public record AsignarGrupoRequest(
        @NotNull(message = "Debes elegir la lectura")
        Long lecturaId,

        @FutureOrPresent(message = "La fecha límite no puede estar en el pasado")
        LocalDate fechaLimite
) {
    // asignación sin fecha límite
    public AsignarGrupoRequest(Long lecturaId) {
        this(lecturaId, null);
    }
}
