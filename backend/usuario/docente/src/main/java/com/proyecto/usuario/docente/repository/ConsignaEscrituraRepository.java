package com.proyecto.usuario.docente.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * T-400 (US-22): palabras mínimas que pide la consigna de un nivel de escritura.
 *
 * <p>La tabla {@code consigna_nivel} la crea el módulo estudiante (Karla,
 * migración V4 de su backend); aquí solo se lee con SQL directo. Si el nivel no
 * tiene consigna, o la tabla no existe en la base (por ejemplo en el CI), se usan
 * {@value #PALABRAS_MINIMAS_POR_DEFECTO} palabras.</p>
 */
@Repository
public class ConsignaEscrituraRepository {

    public static final int PALABRAS_MINIMAS_POR_DEFECTO = 10;

    private static final Logger log = LoggerFactory.getLogger(ConsignaEscrituraRepository.class);

    private final JdbcTemplate jdbc;

    public ConsignaEscrituraRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int palabrasMinimas(Short nivelId) {
        try {
            List<Integer> filas = jdbc.queryForList(
                    "SELECT palabras_minimas FROM consigna_nivel WHERE nivel_id = ?",
                    Integer.class, nivelId);
            if (!filas.isEmpty()) {
                return filas.get(0);
            }
        } catch (DataAccessException e) {
            log.warn("No se pudo leer la consigna del nivel {}; se usan las palabras mínimas por defecto",
                    nivelId, e);
        }
        return PALABRAS_MINIMAS_POR_DEFECTO;
    }
}
