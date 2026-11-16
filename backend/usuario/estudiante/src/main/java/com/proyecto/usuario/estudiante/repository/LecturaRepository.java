package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.Lectura;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LecturaRepository {

    private static final RowMapper<Lectura> LECTURA = (rs, n) -> new Lectura(
            rs.getLong("id"), rs.getString("titulo"), rs.getString("texto"),
            rs.getString("tipo_reto"), rs.getInt("grado"), rs.getString("dificultad"));

    private final JdbcClient jdbc;

    public LecturaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // La última lectura activa que el docente le asignó al alumno para este tipo de reto
    public Optional<Lectura> asignada(long estudianteId, String tipoReto) {
        return jdbc.sql("""
                        SELECT l.id, l.titulo, l.texto, l.tipo_reto, l.grado, l.dificultad
                        FROM asignacion a
                        JOIN lecturas l ON l.id = a.lectura_id
                        WHERE a.estudiante_id = :e AND l.activo AND l.tipo_reto = :tipo
                        ORDER BY a.fecha_asignacion DESC, a.id DESC
                        LIMIT 1
                        """)
                .param("e", estudianteId)
                .param("tipo", tipoReto)
                .query(LECTURA)
                .optional();
    }

    // Si no hay asignación: una lectura activa del grado del alumno, empezando por las más fáciles
    public Optional<Lectura> delCatalogo(long estudianteId, String tipoReto) {
        return jdbc.sql("""
                        SELECT l.id, l.titulo, l.texto, l.tipo_reto, l.grado, l.dificultad
                        FROM estudiante e
                        JOIN lecturas l ON l.grado = e.grado_id
                        WHERE e.id = :e AND l.activo AND l.tipo_reto = :tipo
                        ORDER BY CASE l.dificultad WHEN 'basica' THEN 1 WHEN 'intermedia' THEN 2 ELSE 3 END, l.id
                        LIMIT 1
                        """)
                .param("e", estudianteId)
                .param("tipo", tipoReto)
                .query(LECTURA)
                .optional();
    }
}
