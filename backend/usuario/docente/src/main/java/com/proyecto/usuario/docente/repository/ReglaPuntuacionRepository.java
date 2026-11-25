package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.ReglaPuntuacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Lee las reglas de puntuación del administrador (US-53, Daniela).
 *
 * <p>La tabla {@code reglas_puntuacion} es del módulo Admin: aquí solo se lee,
 * con SQL directo. Si el tipo de reto no tiene fila, la fila no es válida o la
 * tabla no existe en la base (por ejemplo en el CI), se usan
 * {@link ReglaPuntuacion#porDefecto(String)}: 100 puntos y umbrales 60 / 80 / 95.</p>
 */
@Repository
public class ReglaPuntuacionRepository {

    private static final Logger log = LoggerFactory.getLogger(ReglaPuntuacionRepository.class);

    private final JdbcTemplate jdbc;

    public ReglaPuntuacionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ReglaPuntuacion porTipo(String tipoReto) {
        try {
            List<ReglaPuntuacion> filas = jdbc.query(
                    "SELECT puntos, umbral_1_estrella, umbral_2_estrellas, umbral_3_estrellas "
                            + "FROM reglas_puntuacion WHERE tipo_reto = ?",
                    (rs, i) -> new ReglaPuntuacion(tipoReto, rs.getInt("puntos"),
                            rs.getInt("umbral_1_estrella"), rs.getInt("umbral_2_estrellas"),
                            rs.getInt("umbral_3_estrellas")),
                    tipoReto);
            if (!filas.isEmpty()) {
                return filas.get(0);
            }
        } catch (DataAccessException | IllegalArgumentException e) {
            log.warn("No se pudo leer la regla de puntuación de {}; se usa la de por defecto",
                    tipoReto, e);
        }
        return ReglaPuntuacion.porDefecto(tipoReto);
    }
}
