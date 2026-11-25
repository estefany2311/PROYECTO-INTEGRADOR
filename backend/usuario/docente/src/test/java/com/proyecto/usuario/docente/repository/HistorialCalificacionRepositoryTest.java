package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.HistorialCalificacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * T-380 (US-33): prueba el historial de calificaciones contra PostgreSQL.
 * Inserta datos con ids negativos y la transacción de @DataJpaTest hace rollback.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HistorialCalificacionRepositoryTest {

    @Autowired EvidenciaRepository evidenciaRepository;
    @Autowired HistorialCalificacionRepository historialRepository;
    @Autowired EntityManager entityManager;
    @Autowired DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void insertarDatosDePrueba() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO aula (id, nombre, docente_id, codigo)
                VALUES (-2003, 'Aula de prueba T-380', 1, 'T380AA')
                """);
        jdbc.update("INSERT INTO aula_grado (aula_id, grado_id) VALUES (-2003, 1)");
        jdbc.update("""
                INSERT INTO estudiante (id, nombres, apellidos, aula_id, grado_id, activo)
                VALUES (-3201, 'Minji', 'Historial', -2003, 1, true)
                """);
        jdbc.update("""
                INSERT INTO nivel (id, orden, nombre, tipo_reto)
                VALUES (-110, -110, 'Nivel historial T-380', 'lectura')
                """);
        jdbc.update("""
                INSERT INTO evidencia (id, estudiante_id, nivel_id, tipo, audio_url,
                                       estado_revision, puntaje_ia, creado_en)
                VALUES (-5101, -3201, -110, 'audio',
                        'https://cdn.ejemplo.pe/audio.mp3', 'pendiente', 85, now())
                """);
    }

    @Test
    void guardaElHistorialYLoDevuelveDelMasRecienteAlMasAntiguo() {
        Evidencia evidencia = evidenciaRepository.findById(-5101L).orElseThrow();

        historialRepository.save(HistorialCalificacion.registrar(
                evidencia, 1L, Evidencia.APROBADA, (short) 85, (short) 85, null));
        historialRepository.save(HistorialCalificacion.registrar(
                evidencia, 1L, Evidencia.CORREGIDA, (short) 85, (short) 70,
                "El audio tiene ruido de fondo."));

        // la fecha la genera la BD: se recarga desde PostgreSQL
        entityManager.flush();
        entityManager.clear();

        List<HistorialCalificacion> historial = historialRepository
                .findByEvidenciaIdOrderByCreadoEnDescIdDesc(-5101L);

        assertEquals(2, historial.size());
        HistorialCalificacion masReciente = historial.get(0);
        assertEquals(Evidencia.CORREGIDA, masReciente.getAccion());
        assertEquals((short) 85, masReciente.getNotaIa());
        assertEquals((short) 70, masReciente.getNotaDocente());
        assertEquals("El audio tiene ruido de fondo.", masReciente.getMotivo());
        assertEquals(1L, masReciente.getDocenteId());
        assertNotNull(masReciente.getCreadoEn());

        HistorialCalificacion aprobacion = historial.get(1);
        assertEquals(Evidencia.APROBADA, aprobacion.getAccion());
        assertNull(aprobacion.getMotivo());
    }

    @Test
    void borraElHistorialEnCascadaConLaEvidencia() {
        jdbc.update("""
                INSERT INTO historial_calificacion
                    (evidencia_id, docente_id, nota_ia, nota_docente, accion)
                VALUES (-5101, 1, 85, 70, 'corregida')
                """);

        jdbc.update("DELETE FROM evidencia WHERE id = -5101");

        Integer restantes = jdbc.queryForObject(
                "SELECT count(*) FROM historial_calificacion WHERE evidencia_id = -5101",
                Integer.class);
        assertEquals(0, restantes);
    }
}
