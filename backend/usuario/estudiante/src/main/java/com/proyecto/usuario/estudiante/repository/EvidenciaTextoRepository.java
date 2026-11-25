package com.proyecto.usuario.estudiante.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

// T-434 (US-21): evidencias de texto en la tabla evidencia del módulo docente (V10 y V14 de Noemí)
@Repository
public class EvidenciaTextoRepository {

    private final JdbcClient jdbc;

    public EvidenciaTextoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Queda "pendiente" para el docente. palabras_marcadas: los errores del corrector, separados por comas
    public long crear(long estudianteId, int nivelId, String texto, String palabrasMarcadas, LocalDateTime ahora) {
        return jdbc.sql("""
                        INSERT INTO evidencia (estudiante_id, nivel_id, tipo, texto, palabras_marcadas, creado_en)
                        VALUES (:e, :n, 'texto', :texto, :marcadas, :ahora)
                        RETURNING id
                        """)
                .param("e", estudianteId)
                .param("n", nivelId)
                .param("texto", texto)
                .param("marcadas", palabrasMarcadas)
                .param("ahora", ahora)
                .query(Long.class)
                .single();
    }
}
