package com.proyecto.usuario.docente.repository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Biblioteca: sin la tabla de Admin sale vacía, pero un fallo de la base no se esconde
class LibroRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final LibroRepository repositorio = new LibroRepository(jdbc);

    @SuppressWarnings("unchecked")
    @Test
    void siLaTablaNoExisteLaBibliotecaSaleVacia() {
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenThrow(new BadSqlGrammarException("consulta", "SELECT",
                        new SQLException("la relación no existe")));

        assertTrue(repositorio.listar(null).isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void unFalloDeConexionSiSePropaga() {
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("sin conexión"));

        assertThrows(DataAccessResourceFailureException.class, () -> repositorio.listar(null));
    }
}
