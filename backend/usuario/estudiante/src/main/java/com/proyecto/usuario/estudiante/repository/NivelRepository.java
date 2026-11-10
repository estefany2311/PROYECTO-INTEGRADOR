package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.Nivel;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class NivelRepository {

    // Estado y puntos guardados de un alumno en un nivel
    public record Progreso(String estado, int puntos) {}

    private static final RowMapper<Nivel> NIVEL = (rs, n) -> new Nivel(
            rs.getInt("id"), rs.getInt("orden"), rs.getString("nombre"),
            rs.getString("tipo_reto"), rs.getString("descripcion"));

    private final JdbcClient jdbc;

    public NivelRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Nivel> buscarActivo(int nivelId) {
        return jdbc.sql("SELECT id, orden, nombre, tipo_reto, descripcion FROM nivel WHERE id = :id AND activo")
                .param("id", nivelId)
                .query(NIVEL)
                .optional();
    }

    // El nivel activo que va justo antes en el recorrido (vacío si es el primero)
    public Optional<Nivel> anterior(int orden) {
        return jdbc.sql("""
                        SELECT id, orden, nombre, tipo_reto, descripcion FROM nivel
                        WHERE activo AND orden < :orden
                        ORDER BY orden DESC
                        LIMIT 1
                        """)
                .param("orden", orden)
                .query(NIVEL)
                .optional();
    }

    // T-198: el nivel activo que va justo después en el recorrido (vacío si es el último)
    public Optional<Nivel> siguiente(int orden) {
        return jdbc.sql("""
                        SELECT id, orden, nombre, tipo_reto, descripcion FROM nivel
                        WHERE activo AND orden > :orden
                        ORDER BY orden
                        LIMIT 1
                        """)
                .param("orden", orden)
                .query(NIVEL)
                .optional();
    }

    public int marcarCompletado(long estudianteId, int nivelId, int puntos, LocalDateTime ahora) {
        return jdbc.sql("""
                        INSERT INTO progreso (estudiante_id, nivel_id, estado, puntos, actualizado_en)
                        VALUES (:e, :n, 'completado', :puntos, :ahora)
                        ON CONFLICT (estudiante_id, nivel_id)
                        DO UPDATE SET estado = 'completado',
                                      puntos = GREATEST(progreso.puntos, EXCLUDED.puntos),
                                      actualizado_en = EXCLUDED.actualizado_en
                        RETURNING puntos
                        """)
                .param("e", estudianteId)
                .param("n", nivelId)
                .param("puntos", puntos)
                .param("ahora", ahora)
                .query(Integer.class)
                .single();
    }

    public boolean desbloquear(long estudianteId, int nivelId, LocalDateTime ahora) {
        return jdbc.sql("""
                        INSERT INTO progreso (estudiante_id, nivel_id, estado, puntos, actualizado_en)
                        VALUES (:e, :n, 'desbloqueado', 0, :ahora)
                        ON CONFLICT (estudiante_id, nivel_id) DO NOTHING
                        """)
                .param("e", estudianteId)
                .param("n", nivelId)
                .param("ahora", ahora)
                .update() > 0;
    }

    public Optional<Progreso> progreso(long estudianteId, int nivelId) {
        return jdbc.sql("SELECT estado, puntos FROM progreso WHERE estudiante_id = :e AND nivel_id = :n")
                .param("e", estudianteId)
                .param("n", nivelId)
                .query((rs, n) -> new Progreso(rs.getString("estado"), rs.getInt("puntos")))
                .optional();
    }
}
