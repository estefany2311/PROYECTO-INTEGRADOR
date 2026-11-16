package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Lectura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-230: prueba la consulta REAL del catálogo contra PostgreSQL.
 * Inserta lecturas con ids negativos (no chocan con las reales) y la
 * transacción de @DataJpaTest hace rollback al terminar: no deja basura.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogoRepositoryTest {

    @Autowired LecturaRepository lecturaRepository;
    @Autowired DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void insertarLecturasDePrueba() {
        jdbc = new JdbcTemplate(dataSource);
        insertar(-1001L, "Prueba activa 1.º", (short) 1, true);
        insertar(-1002L, "Prueba inactiva 1.º", (short) 1, false);
        insertar(-1003L, "Prueba activa 3.º", (short) 3, true);
    }

    private void insertar(Long id, String titulo, short grado, boolean activo) {
        jdbc.update("""
                INSERT INTO lecturas (id, titulo, texto, tipo_reto, grado, dificultad, activo,
                                      creado_en, actualizado_en)
                VALUES (?, ?, 'Texto de prueba', 'lectura', ?, 'basica', ?, now(), now())
                """, id, titulo, grado, activo);
    }

    @Test
    void soloDevuelveLasActivasDeLosGradosPedidos() {
        List<Lectura> lecturas = lecturaRepository
                .findByActivoTrueAndGradoInOrderByGradoAscTituloAsc(Set.of((short) 1, (short) 2));

        List<Lectura> nuestras = lecturas.stream().filter(l -> l.getId() < 0).toList();

        // la inactiva (-1002) y la de 3.º (-1003) no salen
        assertEquals(List.of(-1001L), nuestras.stream().map(Lectura::getId).toList());
        assertTrue(lecturas.stream().allMatch(Lectura::isActivo));
        assertTrue(lecturas.stream().allMatch(l -> l.getGrado() == 1 || l.getGrado() == 2));
    }

    @Test
    void ordenaPorGradoYTitulo() {
        insertar(-1004L, "Prueba activa 2.º", (short) 2, true);

        List<Lectura> lecturas = lecturaRepository
                .findByActivoTrueAndGradoInOrderByGradoAscTituloAsc(Set.of((short) 1, (short) 2));

        assertEquals(List.of(-1001L, -1004L),
                lecturas.stream().filter(l -> l.getId() < 0).map(Lectura::getId).toList());
    }
}
