package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.dto.EstadoEvidenciaResponse;
import com.proyecto.usuario.estudiante.dto.ResultadoLecturaResponse;
import com.proyecto.usuario.estudiante.model.DatosLectura;
import com.proyecto.usuario.estudiante.model.MejorIntento;
import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.PendienteVoz;
import com.proyecto.usuario.estudiante.model.ResultadoPrecision;
import com.proyecto.usuario.estudiante.model.TextoCalificado;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// T-414: la tabla evidencia es del módulo docente (V10 de Noemí); aquí solo se insertan las del alumno
@Repository
public class EvidenciaRepository {

    private static final Logger log = LoggerFactory.getLogger(EvidenciaRepository.class);

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

    // T-424: datos de la lectura que se grabó, para calificarla
    public Optional<DatosLectura> datosLectura(long evidenciaId) {
        return jdbc.sql("""
                        SELECT e.estudiante_id, e.nivel_id, el.lectura_id, el.texto_lectura, el.duracion_ms
                        FROM evidencia_lectura el
                        JOIN evidencia e ON e.id = el.evidencia_id
                        WHERE el.evidencia_id = :id
                        """)
                .param("id", evidenciaId)
                .query((rs, n) -> new DatosLectura(rs.getLong("estudiante_id"), rs.getInt("nivel_id"),
                        rs.getLong("lectura_id"), rs.getString("texto_lectura"), rs.getLong("duracion_ms")))
                .optional();
    }

    // T-416/T-424: guarda lo que escuchó Whisper y la calificación juntos; recién ahí pasa a "listo"
    @Transactional
    public void guardarResultado(long evidenciaId, Transcripcion transcripcion, ResultadoPrecision precision,
                                 List<PalabraAlineada> alineacion, LocalDateTime ahora) {
        jdbc.sql("""
                        UPDATE evidencia_lectura
                        SET estado_ia = 'listo', transcripcion = :texto, error_ia = NULL, procesado_en = :ahora,
                            precision_pct = :precision, correctas = :correctas, evaluadas = :evaluadas,
                            cobertura_pct = :cobertura, umbral_pct = :umbral, aprobado = :aprobado
                        WHERE evidencia_id = :id
                        """)
                .param("texto", transcripcion.texto())
                .param("ahora", ahora)
                .param("precision", precision.precision())
                .param("correctas", precision.correctas())
                .param("evaluadas", precision.evaluadas())
                .param("cobertura", precision.cobertura())
                .param("umbral", precision.umbral())
                .param("aprobado", precision.aprobado())
                .param("id", evidenciaId)
                .update();
        guardarPalabrasEscuchadas(evidenciaId, transcripcion);
        guardarPalabrasCalificadas(evidenciaId, alineacion);
    }

    // T-424: también en la tabla evidencia de Noemí (V13/V14), que su panel y su puntaje leen.
    // Va aparte: si su migración todavía no corrió en esta base, la calificación ya quedó guardada.
    public void copiarAEvidencia(long evidenciaId, String transcripcion, int puntajeIa, String palabrasMarcadas) {
        try {
            jdbc.sql("""
                            UPDATE evidencia
                            SET transcripcion = :texto, puntaje_ia = :puntaje, palabras_marcadas = :marcadas
                            WHERE id = :id
                            """)
                    .param("texto", transcripcion)
                    .param("puntaje", puntajeIa)
                    .param("marcadas", palabrasMarcadas)
                    .param("id", evidenciaId)
                    .update();
        } catch (DataAccessException e) {
            log.warn("No se pudo copiar el resultado a evidencia {}: {}", evidenciaId, e.getMessage());
        }
    }

    // T-425: estrellas y puntos del intento, y el nivel que se abrió (null si no se abrió ninguno nuevo)
    public void guardarEstrellas(long evidenciaId, int estrellas, int puntos, Integer nivelDesbloqueado) {
        jdbc.sql("""
                        UPDATE evidencia_lectura
                        SET estrellas = :estrellas, puntos = :puntos, nivel_desbloqueado = :nivel
                        WHERE evidencia_id = :id
                        """)
                .param("estrellas", estrellas)
                .param("puntos", puntos)
                .param("nivel", nivelDesbloqueado)
                .param("id", evidenciaId)
                .update();
    }

    // T-425: resultado para la app; solo si la evidencia es del alumno que pregunta
    public Optional<ResultadoLecturaResponse> resultado(long evidenciaId, long estudianteId) {
        return jdbc.sql("""
                        SELECT el.evidencia_id, e.nivel_id, el.estado_ia, el.transcripcion,
                               el.precision_pct, el.correctas, el.evaluadas, el.cobertura_pct, el.umbral_pct,
                               el.aprobado, el.estrellas, el.puntos, el.nivel_desbloqueado, n.nombre AS nivel_nombre,
                               (SELECT a.id FROM asignacion a
                                WHERE a.estudiante_id = e.estudiante_id AND a.lectura_id = el.lectura_id
                                ORDER BY a.fecha_asignacion DESC, a.id DESC LIMIT 1) AS asignacion_id
                        FROM evidencia_lectura el
                        JOIN evidencia e ON e.id = el.evidencia_id
                        LEFT JOIN nivel n ON n.id = el.nivel_desbloqueado
                        WHERE el.evidencia_id = :id AND e.estudiante_id = :estudiante
                        """)
                .param("id", evidenciaId)
                .param("estudiante", estudianteId)
                .query((rs, n) -> new ResultadoLecturaResponse(
                        rs.getLong("evidencia_id"), rs.getInt("nivel_id"), rs.getString("estado_ia"),
                        rs.getString("transcripcion"),
                        rs.getObject("precision_pct", Integer.class), rs.getObject("correctas", Integer.class),
                        rs.getObject("evaluadas", Integer.class), rs.getObject("cobertura_pct", Integer.class),
                        rs.getObject("umbral_pct", Integer.class), rs.getObject("aprobado", Boolean.class),
                        rs.getObject("estrellas", Integer.class), rs.getObject("puntos", Integer.class),
                        rs.getObject("nivel_desbloqueado", Integer.class), rs.getString("nivel_nombre"),
                        rs.getObject("asignacion_id", Long.class), List.of()))
                .optional();
    }

    // T-432 (US-16): el mejor de sus intentos anteriores en el mismo nivel (solo los ya calificados).
    // El mejor puntaje del nivel ya se guarda en progreso (T-310); aquí se compara la precisión
    public MejorIntento mejorAnterior(long evidenciaId) {
        return jdbc.sql("""
                        SELECT count(*) AS intentos, max(el.precision_pct) AS mejor_precision,
                               max(el.estrellas) AS mejor_estrellas
                        FROM evidencia actual
                        JOIN evidencia e ON e.estudiante_id = actual.estudiante_id AND e.nivel_id = actual.nivel_id
                        JOIN evidencia_lectura el ON el.evidencia_id = e.id
                        WHERE actual.id = :id AND e.id < actual.id AND el.estado_ia = 'listo'
                        """)
                .param("id", evidenciaId)
                .query((rs, n) -> new MejorIntento(rs.getInt("intentos"),
                        rs.getObject("mejor_precision", Integer.class), rs.getObject("mejor_estrellas", Integer.class)))
                .single();
    }

    // T-428: el texto que leyó y el tipo de cada palabra del texto, para marcar las que debe mejorar
    public Optional<TextoCalificado> textoCalificado(long evidenciaId) {
        return jdbc.sql("SELECT texto_lectura FROM evidencia_lectura WHERE evidencia_id = :id")
                .param("id", evidenciaId)
                .query(String.class)
                .optional()
                .map(texto -> new TextoCalificado(texto, jdbc.sql("""
                                SELECT tipo FROM evidencia_resultado_palabra
                                WHERE evidencia_id = :id AND tipo <> 'agregada'
                                ORDER BY orden
                                """)
                        .param("id", evidenciaId)
                        .query(String.class)
                        .list()));
    }

    // T-416: cada palabra que escuchó Whisper con sus tiempos (la fluidez de Noemí las lee)
    private void guardarPalabrasEscuchadas(long evidenciaId, Transcripcion transcripcion) {
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

    // T-424: cada palabra del texto con lo que leyó el niño (correcta, cambiada, omitida o agregada)
    private void guardarPalabrasCalificadas(long evidenciaId, List<PalabraAlineada> alineacion) {
        jdbc.sql("DELETE FROM evidencia_resultado_palabra WHERE evidencia_id = :id").param("id", evidenciaId).update();
        for (int orden = 0; orden < alineacion.size(); orden++) {
            PalabraAlineada p = alineacion.get(orden);
            jdbc.sql("""
                            INSERT INTO evidencia_resultado_palabra (evidencia_id, orden, esperada, leida, tipo)
                            VALUES (:id, :orden, :esperada, :leida, :tipo)
                            """)
                    .param("id", evidenciaId)
                    .param("orden", orden)
                    .param("esperada", p.esperada() == null ? null : recortar(p.esperada(), 60))
                    .param("leida", p.leida() == null ? null : recortar(p.leida(), 60))
                    .param("tipo", p.tipo().name().toLowerCase())
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
