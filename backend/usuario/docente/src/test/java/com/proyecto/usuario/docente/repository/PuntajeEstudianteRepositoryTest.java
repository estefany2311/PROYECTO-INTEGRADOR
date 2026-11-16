package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.PuntajeEstudiante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-305 (US-09): prueba la tabla real de puntos y estrellas contra PostgreSQL.
 * Inserta datos con ids negativos y la transacción de @DataJpaTest hace rollback.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PuntajeEstudianteRepositoryTest {

    @Autowired PuntajeEstudianteRepository puntajeEstudianteRepository;
    @Autowired DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void insertarDatosDePrueba() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO aula (id, nombre, docente_id, codigo)
                VALUES (-2102, 'Aula de prueba T-305', 1, 'T305AA')
                """);
        jdbc.update("INSERT INTO aula_grado (aula_id, grado_id) VALUES (-2102, 1)");
        jdbc.update("""
                INSERT INTO estudiante (id, nombres, apellidos, aula_id, grado_id, activo)
                VALUES (-3201, 'Minji', 'Prueba', -2102, 1, true)
                """);
        jdbc.update("""
                INSERT INTO puntaje_estudiante (id, estudiante_id, puntos, estrellas)
                VALUES (-6001, -3201, 350, 6)
                """);
    }

    @Test
    void encuentraElAcumuladoConElAlumno() {
        Optional<PuntajeEstudiante> encontrado =
                puntajeEstudianteRepository.findByEstudianteId(-3201L);

        assertTrue(encontrado.isPresent());
        assertEquals(350, encontrado.get().getPuntos());
        assertEquals(6, encontrado.get().getEstrellas());
        assertEquals("Minji", encontrado.get().getEstudiante().getNombres());
        assertNotNull(encontrado.get().getActualizadoEn());
    }

    @Test
    void devuelveVacioSiElAlumnoNoTieneAcumulado() {
        assertTrue(puntajeEstudianteRepository.findByEstudianteId(-9999L).isEmpty());
    }

    @Test
    void unaFilaNuevaArrancaEnCero() {
        jdbc.update("""
                INSERT INTO estudiante (id, nombres, apellidos, aula_id, grado_id, activo)
                VALUES (-3202, 'Breysi', 'Zavala', -2102, 1, true)
                """);
        jdbc.update("""
                INSERT INTO puntaje_estudiante (id, estudiante_id)
                VALUES (-6002, -3202)
                """);

        PuntajeEstudiante nuevo =
                puntajeEstudianteRepository.findByEstudianteId(-3202L).orElseThrow();

        assertEquals(0, nuevo.getPuntos());
        assertEquals(0, nuevo.getEstrellas());
        assertNotNull(nuevo.getActualizadoEn());
    }

    @Test
    void noPermiteDosAcumuladosParaElMismoAlumno() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO puntaje_estudiante (id, estudiante_id, puntos, estrellas)
                VALUES (-6002, -3201, 0, 0)
                """));
    }

    @Test
    void borrarElAlumnoLimpiaSuPuntaje() {
        jdbc.update("DELETE FROM estudiante WHERE id = -3201");

        assertTrue(puntajeEstudianteRepository.findByEstudianteId(-3201L).isEmpty());
    }
}
