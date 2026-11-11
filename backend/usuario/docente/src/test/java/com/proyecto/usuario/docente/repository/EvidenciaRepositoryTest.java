package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Evidencia;
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
 * T-298 (US-31): prueba las consultas REALES de evidencias pendientes contra
 * PostgreSQL. Inserta datos con ids negativos y la transaccion de @DataJpaTest
 * hace rollback.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EvidenciaRepositoryTest {

    @Autowired EvidenciaRepository evidenciaRepository;
    @Autowired DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void insertarDatosDePrueba() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO aula (id, nombre, docente_id, codigo)
                VALUES (-2002, 'Aula de prueba T-298', 1, 'T298AA')
                """);
        jdbc.update("INSERT INTO aula_grado (aula_id, grado_id) VALUES (-2002, 1)");
        jdbc.update("""
                INSERT INTO estudiante (id, nombres, apellidos, aula_id, grado_id, activo)
                VALUES (-3101, 'Minji', 'Prueba', -2002, 1, true)
                """);

        jdbc.update("""
                INSERT INTO nivel (id, orden, nombre, tipo_reto)
                VALUES (-101, -101, 'Nivel audio T-298', 'lectura'),
                       (-102, -102, 'Nivel texto T-298', 'escritura')
                """);

        insertarEvidencia(-5001, -101, "audio", Evidencia.PENDIENTE, 3);
        insertarEvidencia(-5002, -102, "texto", Evidencia.PENDIENTE, 1);
        insertarEvidencia(-5003, -101, "audio", Evidencia.APROBADA, 2);
    }

    private void insertarEvidencia(long id, long nivelId, String tipo,
                                   String estado, int diasAtras) {
        jdbc.update("""
                INSERT INTO evidencia (id, estudiante_id, nivel_id, tipo, audio_url,
                                       texto, estado_revision, creado_en)
                VALUES (?, -3101, ?, ?,
                        CASE WHEN ? = 'audio' THEN 'https://cdn.ejemplo.pe/audio.mp3' END,
                        CASE WHEN ? = 'texto' THEN 'Texto de prueba' END,
                        ?, now() - (? * interval '1 day'))
                """, id, nivelId, tipo, tipo, tipo, estado, diasAtras);
    }

    @Test
    void devuelveSoloLasPendientesDeLaMasRecienteALaMasAntigua() {
        List<Evidencia> pendientes = evidenciaRepository
                .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                        -2002L, Evidencia.PENDIENTE);

        assertEquals(2, pendientes.size());
        assertEquals(-5002L, pendientes.get(0).getId());
        assertEquals("texto", pendientes.get(0).getTipo());
        assertEquals(-5001L, pendientes.get(1).getId());
        assertEquals("audio", pendientes.get(1).getTipo());
        assertEquals("Minji", pendientes.get(0).getEstudiante().getNombres());
        assertEquals("Nivel texto T-298", pendientes.get(0).getNivel().getNombre());
    }

    @Test
    void filtraLasPendientesPorTipo() {
        List<Evidencia> audio = evidenciaRepository
                .findByEstudianteAulaIdAndEstadoRevisionAndTipoOrderByCreadoEnDescIdDesc(
                        -2002L, Evidencia.PENDIENTE, Evidencia.AUDIO);
        List<Evidencia> texto = evidenciaRepository
                .findByEstudianteAulaIdAndEstadoRevisionAndTipoOrderByCreadoEnDescIdDesc(
                        -2002L, Evidencia.PENDIENTE, Evidencia.TEXTO);

        assertEquals(1, audio.size());
        assertEquals(-5001L, audio.get(0).getId());
        assertEquals(1, texto.size());
        assertEquals(-5002L, texto.get(0).getId());
    }
}
