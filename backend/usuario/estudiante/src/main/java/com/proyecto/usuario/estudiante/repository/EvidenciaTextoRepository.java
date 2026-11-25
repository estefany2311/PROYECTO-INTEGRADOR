package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.TextoCalificadoIa;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

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

    // T-435: el puntaje de escritura (puntaje_ia) solo si el texto es de ese alumno
    public Optional<TextoCalificadoIa> puntaje(long evidenciaId, long estudianteId) {
        return jdbc.sql("""
                        SELECT nivel_id, puntaje_ia FROM evidencia
                        WHERE id = :id AND estudiante_id = :e AND tipo = 'texto'
                        """)
                .param("id", evidenciaId)
                .param("e", estudianteId)
                .query((rs, n) -> new TextoCalificadoIa(rs.getInt("nivel_id"), rs.getObject("puntaje_ia", Integer.class)))
                .optional();
    }
}
