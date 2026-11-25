package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.ReglaPuntuacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// T-425: lee los puntos y umbrales de estrellas del admin de Daniela (US-53)
@Repository
public class ReglasPuntuacionRepository {

    private static final Logger log = LoggerFactory.getLogger(ReglasPuntuacionRepository.class);

    private final JdbcClient jdbc;

    public ReglasPuntuacionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public ReglaPuntuacion de(String tipoReto) {
        try {
            return jdbc.sql("""
                            SELECT puntos, umbral_1_estrella, umbral_2_estrellas, umbral_3_estrellas
                            FROM reglas_puntuacion WHERE tipo_reto = :tipo
                            """)
                    .param("tipo", tipoReto)
                    .query((rs, n) -> new ReglaPuntuacion(rs.getInt("puntos"), rs.getInt("umbral_1_estrella"),
                            rs.getInt("umbral_2_estrellas"), rs.getInt("umbral_3_estrellas")))
                    .optional()
                    .orElse(ReglaPuntuacion.POR_DEFECTO);
        } catch (DataAccessException e) {
            log.warn("No se pudo leer reglas_puntuacion: {}", e.getMessage());
            return ReglaPuntuacion.POR_DEFECTO;
        }
    }
}
