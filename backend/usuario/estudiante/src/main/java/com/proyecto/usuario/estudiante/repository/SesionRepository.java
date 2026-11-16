package com.proyecto.usuario.estudiante.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class SesionRepository {

    private final JdbcClient jdbc;

    public SesionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void guardar(String tokenHash, long estudianteId, LocalDateTime creadaEn, LocalDateTime expiraEn) {
        jdbc.sql("""
                        INSERT INTO sesion_estudiante (token_hash, estudiante_id, creada_en, expira_en)
                        VALUES (:hash, :estudiante, :creada, :expira)
                        """)
                .param("hash", tokenHash)
                .param("estudiante", estudianteId)
                .param("creada", creadaEn)
                .param("expira", expiraEn)
                .update();
    }

    // Devuelve el id del estudiante si el token existe y no ha vencido (lo usarás en T-208)
    public Optional<Long> buscarEstudianteVigente(String tokenHash, LocalDateTime ahora) {
        return jdbc.sql("SELECT estudiante_id FROM sesion_estudiante WHERE token_hash = :hash AND expira_en > :ahora")
                .param("hash", tokenHash)
                .param("ahora", ahora)
                .query(Long.class)
                .optional();
    }

    public void eliminar(String tokenHash) {
        jdbc.sql("DELETE FROM sesion_estudiante WHERE token_hash = :hash")
                .param("hash", tokenHash)
                .update();
    }
}