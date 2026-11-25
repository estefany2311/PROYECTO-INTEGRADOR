package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.ConfiguracionFluidez;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * T-392 (US-15): lee la configuración de voz del administrador (US-51).
 *
 * <p>La tabla {@code configuracion_evaluacion} es del módulo Admin (Daniela):
 * aquí solo se lee, con SQL directo, igual que la sesión del alumno. Si el grado
 * no tiene fila, o la tabla no existe en la base (por ejemplo en el CI), se usan
 * los valores iniciales de {@link ConfiguracionFluidez#porDefecto(int)}.</p>
 */
@Repository
public class ConfiguracionFluidezRepository {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionFluidezRepository.class);

    private final JdbcTemplate jdbc;

    public ConfiguracionFluidezRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ConfiguracionFluidez porGrado(int grado) {
        try {
            List<ConfiguracionFluidez> filas = jdbc.query(
                    "SELECT palabras_por_minuto, tolerancia_pausas "
                            + "FROM configuracion_evaluacion WHERE grado = ?",
                    (rs, i) -> new ConfiguracionFluidez(
                            grado, rs.getInt("palabras_por_minuto"),
                            rs.getBigDecimal("tolerancia_pausas").doubleValue()),
                    grado);
            if (!filas.isEmpty()) {
                return filas.get(0);
            }
        } catch (DataAccessException e) {
            log.warn("No se pudo leer la configuración de voz del grado {}; se usan los valores iniciales",
                    grado, e);
        }
        return ConfiguracionFluidez.porDefecto(grado);
    }
}
