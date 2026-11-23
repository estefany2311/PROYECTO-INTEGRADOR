package com.proyecto.usuario.estudiante.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// T-423: lee la configuración de evaluación de voz de Daniela (US-51), por el grado del alumno
@Repository
public class ConfiguracionEvaluacionRepository {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionEvaluacionRepository.class);

    private final JdbcClient jdbc;

    public ConfiguracionEvaluacionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Integer> umbralPrecision(long estudianteId) {
        try {
            return jdbc.sql("""
                            SELECT ce.umbral_precision
                            FROM estudiante e
                            JOIN grado g ON g.id = e.grado_id
                            JOIN configuracion_evaluacion ce ON ce.grado = g.orden
                            WHERE e.id = :estudiante
                            """)
                    .param("estudiante", estudianteId)
                    .query(Integer.class)
                    .optional();
        } catch (DataAccessException e) {
            // Si la tabla del admin todavía no existe en esta base, se usa el valor por defecto
            log.warn("No se pudo leer configuracion_evaluacion: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
