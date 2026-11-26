package com.proyecto.usuario.docente.dto;

/**
 * Biblioteca: un libro oficial del Minedu tal como lo ve el docente.
 *
 * @param grado        1 a 6; nulo si el recurso no indica grado
 * @param competencia  lectura, escritura u oralidad; vacío si no la indica
 * @param tienePdf     si trae enlace a un PDF que se pueda abrir
 */
public record LibroResponse(
        Long id,
        String titulo,
        String descripcion,
        Integer grado,
        String competencia,
        boolean tienePdf
) {
}
