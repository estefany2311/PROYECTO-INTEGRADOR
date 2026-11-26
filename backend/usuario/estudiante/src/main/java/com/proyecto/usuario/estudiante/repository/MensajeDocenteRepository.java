package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.dto.MensajeDocenteResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

// Lee los comentarios de la tabla evidencia del módulo docente; no escribe nada en ella
@Repository
public class MensajeDocenteRepository {

    static final int MAXIMO = 30;

    private final JdbcClient jdbc;

    public MensajeDocenteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Los más recientes primero; solo los que tienen un comentario escrito
    public List<MensajeDocenteResponse> delEstudiante(long estudianteId) {
        return jdbc.sql("""
                        SELECT e.id, e.nivel_id, n.nombre AS nivel_nombre, e.tipo, e.comentario,
                               COALESCE(e.revisado_en, e.creado_en) AS fecha
                        FROM evidencia e
                        JOIN nivel n ON n.id = e.nivel_id
                        WHERE e.estudiante_id = :e
                          AND e.comentario IS NOT NULL AND btrim(e.comentario) <> ''
                        ORDER BY fecha DESC, e.id DESC
                        LIMIT :maximo
                        """)
                .param("e", estudianteId)
                .param("maximo", MAXIMO)
                .query((rs, n) -> new MensajeDocenteResponse(
                        rs.getLong("id"),
                        rs.getInt("nivel_id"),
                        rs.getString("nivel_nombre"),
                        rs.getString("tipo"),
                        rs.getString("comentario").strip(),
                        rs.getObject("fecha", java.time.LocalDateTime.class)))
                .list();
    }
}
