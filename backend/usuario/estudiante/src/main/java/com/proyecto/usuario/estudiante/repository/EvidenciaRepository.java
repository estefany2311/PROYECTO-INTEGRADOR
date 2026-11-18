package com.proyecto.usuario.estudiante.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

// T-414: la tabla evidencia es del módulo docente (V10 de Noemí); aquí solo se insertan las del alumno
@Repository
public class EvidenciaRepository {

    private final JdbcClient jdbc;

    public EvidenciaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Queda en estado "pendiente" (valor por defecto) para que el docente la revise
    public long crearAudio(long estudianteId, int nivelId, String audioUrl, LocalDateTime ahora) {
        return jdbc.sql("""
                        INSERT INTO evidencia (estudiante_id, nivel_id, tipo, audio_url, creado_en)
                        VALUES (:e, :n, 'audio', :url, :ahora)
                        RETURNING id
                        """)
                .param("e", estudianteId)
                .param("n", nivelId)
                .param("url", audioUrl)
                .param("ahora", ahora)
                .query(Long.class)
                .single();
    }

    public void registrarLectura(long evidenciaId, long lecturaId, String textoLectura,
                                 long duracionMs, int tamanoBytes, LocalDateTime ahora) {
        jdbc.sql("""
                        INSERT INTO evidencia_lectura (evidencia_id, lectura_id, texto_lectura, duracion_ms, tamano_bytes, creado_en)
                        VALUES (:id, :lectura, :texto, :duracion, :tamano, :ahora)
                        """)
                .param("id", evidenciaId)
                .param("lectura", lecturaId)
                .param("texto", textoLectura)
                .param("duracion", duracionMs)
                .param("tamano", tamanoBytes)
                .param("ahora", ahora)
                .update();
    }
}
