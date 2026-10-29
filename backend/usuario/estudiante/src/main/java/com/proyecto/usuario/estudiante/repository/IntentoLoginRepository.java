package com.proyecto.usuario.estudiante.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class IntentoLoginRepository {

    private final JdbcClient jdbc;

    public IntentoLoginRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<LocalDateTime> bloqueadoHasta(long estudianteId) {
        return jdbc.sql("SELECT bloqueado_hasta FROM intento_login WHERE estudiante_id = :id AND bloqueado_hasta IS NOT NULL")
                .param("id", estudianteId)
                .query((rs, n) -> rs.getTimestamp("bloqueado_hasta").toLocalDateTime())
                .optional();
    }

    // Suma un fallo y devuelve cuántos lleva seguidos
    public int registrarFallo(long estudianteId) {
        return jdbc.sql("""
                        INSERT INTO intento_login (estudiante_id, fallidos) VALUES (:id, 1)
                        ON CONFLICT (estudiante_id)
                        DO UPDATE SET fallidos = intento_login.fallidos + 1, bloqueado_hasta = NULL
                        RETURNING fallidos
                        """)
                .param("id", estudianteId)
                .query(Integer.class)
                .single();
    }

    public void bloquear(long estudianteId, LocalDateTime hasta) {
        jdbc.sql("UPDATE intento_login SET fallidos = 0, bloqueado_hasta = :hasta WHERE estudiante_id = :id")
                .param("id", estudianteId)
                .param("hasta", hasta)
                .update();
    }

    public void limpiar(long estudianteId) {
        jdbc.sql("DELETE FROM intento_login WHERE estudiante_id = :id")
                .param("id", estudianteId)
                .update();
    }
}
