package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.ReglasOrtografia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

// T-328/T-329: lee la configuración de Daniela (US-52) y el diccionario por grado (V5)
@Repository
public class OrtografiaRepository {

    private static final Logger log = LoggerFactory.getLogger(OrtografiaRepository.class);

    private final JdbcClient jdbc;

    public OrtografiaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // configuracion_ortografia usa el grado 1 a 6 (grado.orden del módulo docente)
    public Optional<ReglasOrtografia> reglasDe(long estudianteId) {
        try {
            return jdbc.sql("""
                            SELECT co.sensibilidad, co.ignorar_tildes
                            FROM estudiante e
                            JOIN grado g ON g.id = e.grado_id
                            JOIN configuracion_ortografia co ON co.grado = g.orden
                            WHERE e.id = :estudiante
                            """)
                    .param("estudiante", estudianteId)
                    .query((rs, n) -> new ReglasOrtografia(rs.getString("sensibilidad"), rs.getBoolean("ignorar_tildes")))
                    .optional();
        } catch (DataAccessException e) {
            // Si la tabla del admin todavía no existe en esta base, se usan las reglas por defecto
            log.warn("No se pudo leer configuracion_ortografia: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // Palabras que nunca son error: las del admin (para todos) y las del grado del alumno
    public Set<String> palabrasPermitidas(long estudianteId) {
        Set<String> palabras = new HashSet<>();
        agregar(palabras, """
                SELECT palabra FROM palabras_permitidas
                """, null);
        agregar(palabras, """
                SELECT d.palabra
                FROM estudiante e
                JOIN grado g ON g.id = e.grado_id
                JOIN diccionario_grado d ON d.grado = g.orden
                WHERE e.id = :estudiante
                """, estudianteId);
        return palabras;
    }

    private void agregar(Set<String> palabras, String sql, Long estudianteId) {
        try {
            var consulta = jdbc.sql(sql);
            if (estudianteId != null) consulta = consulta.param("estudiante", estudianteId);
            consulta.query(String.class).list().forEach(p -> {
                // "Inti Raymi" se guarda como dos palabras
                for (String parte : p.toLowerCase(Locale.ROOT).split("\\s+")) {
                    if (!parte.isBlank()) palabras.add(parte);
                }
            });
        } catch (DataAccessException e) {
            log.warn("No se pudieron leer las palabras permitidas: {}", e.getMessage());
        }
    }
}
