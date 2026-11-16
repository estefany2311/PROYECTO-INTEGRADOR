package com.proyecto.usuario.docente.dto;

/**
 * T-195 (US-37): resultado de asignar una lectura a un grupo.
 * `yaTenían` cuenta a los alumnos que ya tenían esa lectura y se omitieron,
 * para que repetir la acción no falle ni duplique tareas.
 */
public record AsignacionGrupoResponse(
        Long grupoId,
        Long lecturaId,
        int asignados,
        int yaTenían
) {
}
