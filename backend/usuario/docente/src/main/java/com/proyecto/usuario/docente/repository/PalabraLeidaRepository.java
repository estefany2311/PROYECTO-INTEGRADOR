package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.PalabraLeida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * T-393 (US-15): marcas de tiempo por palabra de una lectura en voz alta.
 *
 * <p>La tabla {@code evidencia_palabra} la crea y la llena el módulo estudiante
 * (Karla, migración V7 de su backend) cuando el motor de voz transcribe el
 * audio: una fila por palabra escuchada, en orden. Aquí solo se lee, con SQL
 * directo, como la sesión del alumno. Si la tabla no existe en la base (por
 * ejemplo en el CI), se devuelve una lista vacía.</p>
 */
@Repository
public class PalabraLeidaRepository {

    private static final Logger log = LoggerFactory.getLogger(PalabraLeidaRepository.class);

    private final JdbcTemplate jdbc;

    public PalabraLeidaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PalabraLeida> porEvidencia(Long evidenciaId) {
        try {
            return jdbc.query(
                    "SELECT palabra, inicio, fin, probabilidad "
                            + "FROM evidencia_palabra WHERE evidencia_id = ? ORDER BY orden",
                    (rs, i) -> new PalabraLeida(
                            rs.getString("palabra"),
                            rs.getBigDecimal("inicio").doubleValue(),
                            rs.getBigDecimal("fin").doubleValue(),
                            rs.getBigDecimal("probabilidad").doubleValue()),
                    evidenciaId);
        } catch (DataAccessException e) {
            log.warn("No se pudieron leer las palabras de la evidencia {}", evidenciaId, e);
            return List.of();
        }
    }
}
