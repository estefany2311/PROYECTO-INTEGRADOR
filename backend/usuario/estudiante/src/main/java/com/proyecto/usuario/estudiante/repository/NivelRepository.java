package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.Nivel;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * T-214: lectura de las tablas `nivel` y `progreso` de la base compartida.
 * Las crea y llena el backend docente (V7); este módulo no las modifica.
 */
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

    public Optional<Progreso> progreso(long estudianteId, int nivelId) {
        return jdbc.sql("SELECT estado, puntos FROM progreso WHERE estudiante_id = :e AND nivel_id = :n")
                .param("e", estudianteId)
                .param("n", nivelId)
                .query((rs, n) -> new Progreso(rs.getString("estado"), rs.getInt("puntos")))
                .optional();
    }
}
