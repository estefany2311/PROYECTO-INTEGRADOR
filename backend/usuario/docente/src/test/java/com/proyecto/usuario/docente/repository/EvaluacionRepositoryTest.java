package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Evaluacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T-276: prueba la consulta REAL de promedios por competencia contra PostgreSQL.
 * Inserta datos con ids negativos y la transaccion de @DataJpaTest hace rollback.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EvaluacionRepositoryTest {

    @Autowired EvaluacionRepository evaluacionRepository;
    @Autowired DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void insertarDatosDePrueba() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO aula (id, nombre, docente_id, codigo)
                VALUES (-2001, 'Aula de prueba T-276', 1, 'T276AA')
                """);
        jdbc.update("INSERT INTO aula_grado (aula_id, grado_id) VALUES (-2001, 1)");
        insertarEstudiante(-3001, "Ana", true);
        insertarEstudiante(-3002, "Beto", true);
        insertarEstudiante(-3003, "Inactivo", false);

        insertarEvaluacion(-4001, -3001, "lectura", 80);
        insertarEvaluacion(-4002, -3001, "lectura", 90);
        insertarEvaluacion(-4003, -3001, "escritura", 70);
        insertarEvaluacion(-4004, -3002, "oralidad", 60);
        insertarEvaluacion(-4005, -3003, "lectura", 100); // alumno inactivo: no debe salir
    }

    private void insertarEstudiante(long id, String nombres, boolean activo) {
        jdbc.update("""
                INSERT INTO estudiante (id, nombres, apellidos, aula_id, grado_id, activo)
                VALUES (?, ?, 'Prueba', -2001, 1, ?)
                """, id, nombres, activo);
    }

    private void insertarEvaluacion(long id, long estudianteId, String competencia, int puntaje) {
        jdbc.update("""
                INSERT INTO evaluacion (id, estudiante_id, competencia, puntaje)
                VALUES (?, ?, ?, ?)
                """, id, estudianteId, competencia, puntaje);
    }

    @Test
    void promediaPorCompetenciaYSoloIncluyeAlumnosActivos() {
        List<PromedioCompetenciaProjection> promedios = evaluacionRepository.promediosPorAula(-2001L);

        assertEquals(3, promedios.size());

        // orden por estudiante_id ascendente: -3002 va antes que -3001
        assertEquals(-3002L, promedios.get(0).getEstudianteId());
        assertEquals("oralidad", promedios.get(0).getCompetencia());
        assertEquals(60.0, promedios.get(0).getPromedio(), 0.001);

        assertEquals(-3001L, promedios.get(1).getEstudianteId());
        assertEquals("escritura", promedios.get(1).getCompetencia());
        assertEquals(70.0, promedios.get(1).getPromedio(), 0.001);

        assertEquals(-3001L, promedios.get(2).getEstudianteId());
        assertEquals("lectura", promedios.get(2).getCompetencia());
        assertEquals(85.0, promedios.get(2).getPromedio(), 0.001);
    }

    @Test
    void devuelveElHistorialDelAlumnoDeLaEvaluacionMasRecienteALaMasAntigua() {
        List<Evaluacion> historial = evaluacionRepository
                .findByEstudianteIdOrderByEvaluadoEnDescIdDesc(-3001L);

        assertEquals(3, historial.size());
        assertEquals(-4001L, historial.get(0).getId());
        assertEquals("lectura", historial.get(0).getCompetencia());
        assertEquals(-4002L, historial.get(1).getId());
        assertEquals(-4003L, historial.get(2).getId());
        assertEquals("escritura", historial.get(2).getCompetencia());
    }
}
