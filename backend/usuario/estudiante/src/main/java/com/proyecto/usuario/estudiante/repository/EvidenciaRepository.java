package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.dto.EstadoEvidenciaResponse;
import com.proyecto.usuario.estudiante.model.PendienteVoz;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // T-416: guarda el texto que escuchó Whisper y sus palabras, y pasa a "listo"
    @Transactional
    public void guardarTranscripcion(long evidenciaId, Transcripcion transcripcion, LocalDateTime ahora) {
        jdbc.sql("""
                        UPDATE evidencia_lectura
                        SET estado_ia = 'listo', transcripcion = :texto, error_ia = NULL, procesado_en = :ahora
                        WHERE evidencia_id = :id
                        """)
                .param("texto", transcripcion.texto())
                .param("ahora", ahora)
                .param("id", evidenciaId)
                .update();
        jdbc.sql("DELETE FROM evidencia_palabra WHERE evidencia_id = :id").param("id", evidenciaId).update();
        List<Transcripcion.Palabra> palabras = transcripcion.palabras() == null ? List.of() : transcripcion.palabras();
        for (int orden = 0; orden < palabras.size(); orden++) {
            Transcripcion.Palabra p = palabras.get(orden);
            jdbc.sql("""
                            INSERT INTO evidencia_palabra (evidencia_id, orden, palabra, inicio, fin, probabilidad)
                            VALUES (:id, :orden, :palabra, :inicio, :fin, :prob)
                            """)
                    .param("id", evidenciaId)
                    .param("orden", orden)
                    .param("palabra", recortar(p.palabra(), 60))
                    .param("inicio", p.inicio())
                    .param("fin", p.fin())
                    .param("prob", p.probabilidad())
                    .update();
        }
    }

    public void marcarError(long evidenciaId, String mensaje, LocalDateTime ahora) {
        jdbc.sql("""
                        UPDATE evidencia_lectura
                        SET estado_ia = 'error', error_ia = :mensaje, procesado_en = :ahora
                        WHERE evidencia_id = :id
                        """)
                .param("mensaje", recortar(mensaje, 300))
                .param("ahora", ahora)
                .param("id", evidenciaId)
                .update();
    }

    // Solo devuelve la evidencia si es del alumno que pregunta
    public Optional<EstadoEvidenciaResponse> estado(long evidenciaId, long estudianteId) {
        return jdbc.sql("""
                        SELECT el.evidencia_id, el.estado_ia, el.transcripcion, el.error_ia
                        FROM evidencia_lectura el
                        JOIN evidencia e ON e.id = el.evidencia_id
                        WHERE el.evidencia_id = :id AND e.estudiante_id = :estudiante
                        """)
                .param("id", evidenciaId)
                .param("estudiante", estudianteId)
                .query((rs, n) -> new EstadoEvidenciaResponse(
                        rs.getLong("evidencia_id"), rs.getString("estado_ia"),
                        rs.getString("transcripcion"), rs.getString("error_ia")))
                .optional();
    }

    public List<PendienteVoz> pendientes() {
        return jdbc.sql("""
                        SELECT el.evidencia_id, e.audio_url
                        FROM evidencia_lectura el
                        JOIN evidencia e ON e.id = el.evidencia_id
                        WHERE el.estado_ia = 'procesando'
                        ORDER BY el.evidencia_id
                        """)
                .query((rs, n) -> new PendienteVoz(rs.getLong("evidencia_id"), rs.getString("audio_url")))
                .list();
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) return "";
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
