package com.proyecto.usuario.estudiante.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * T-308 (US-08): historial de intentos de cada reto (tabla intento_reto, migración V3).
 * Se guarda un intento por cada vez que el alumno termina un reto, con su fecha y su puntaje.
 */
@Repository
public class IntentoRetoRepository {

    private final JdbcClient jdbc;

    public IntentoRetoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(long estudianteId, int nivelId, int puntos, int estrellas, LocalDateTime realizadoEn) {
        jdbc.sql("""
                        INSERT INTO intento_reto (estudiante_id, nivel_id, puntos, estrellas, realizado_en)
                        VALUES (:e, :n, :puntos, :estrellas, :fecha)
                        """)
                .param("e", estudianteId)
                .param("n", nivelId)
                .param("puntos", puntos)
                .param("estrellas", estrellas)
                .param("fecha", realizadoEn)
                .update();
    }
}
