package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.Consigna;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// T-320 (US-17): lee la consigna de un nivel de escritura (tabla consigna_nivel, V4)
@Repository
public class ConsignaRepository {

    private final JdbcClient jdbc;

    public ConsignaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Consigna> delNivel(int nivelId) {
        return jdbc.sql("""
                        SELECT nivel_id, consigna, pista, ilustracion, palabras_minimas
                        FROM consigna_nivel
                        WHERE nivel_id = :nivel
                        """)
                .param("nivel", nivelId)
                .query((rs, n) -> new Consigna(
                        rs.getInt("nivel_id"), rs.getString("consigna"), rs.getString("pista"),
                        rs.getString("ilustracion"), rs.getInt("palabras_minimas")))
                .optional();
    }
}
