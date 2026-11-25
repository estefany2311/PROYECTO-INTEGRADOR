package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.PalabraLeida;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// T-398 (US-15): las palabras vienen en orden de la tabla del módulo estudiante
class PalabraLeidaRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PalabraLeidaRepository repositorio = new PalabraLeidaRepository(jdbc);

    @SuppressWarnings("unchecked")
    @Test
    void devuelveLasPalabrasDeLaEvidencia() {
        List<PalabraLeida> palabras = List.of(
                new PalabraLeida("el", 0.5, 0.8, 0.99),
                new PalabraLeida("sol", 1.0, 1.4, 0.97));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(30L))).thenReturn(palabras);

        assertEquals(palabras, repositorio.porEvidencia(30L));
    }

    @SuppressWarnings("unchecked")
    @Test
    void siLaTablaNoExisteDevuelveUnaListaVacia() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(30L)))
                .thenThrow(new DataAccessResourceFailureException("la relación no existe"));

        assertTrue(repositorio.porEvidencia(30L).isEmpty());
    }
}
