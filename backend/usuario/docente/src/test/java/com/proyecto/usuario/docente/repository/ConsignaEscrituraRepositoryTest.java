package com.proyecto.usuario.docente.repository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// T-404 (US-22): las palabras mínimas salen de la consigna del nivel; si falta, son 10
class ConsignaEscrituraRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ConsignaEscrituraRepository repositorio = new ConsignaEscrituraRepository(jdbc);

    @Test
    void usaLasPalabrasMinimasDeLaConsigna() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), eq((short) 5))).thenReturn(List.of(40));

        assertEquals(40, repositorio.palabrasMinimas((short) 5));
    }

    @Test
    void sinConsignaUsaElValorPorDefecto() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), eq((short) 2))).thenReturn(List.of());

        assertEquals(ConsignaEscrituraRepository.PALABRAS_MINIMAS_POR_DEFECTO,
                repositorio.palabrasMinimas((short) 2));
    }

    @Test
    void siLaTablaNoExisteUsaElValorPorDefecto() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), eq((short) 2)))
                .thenThrow(new DataAccessResourceFailureException("la relación no existe"));

        assertEquals(10, repositorio.palabrasMinimas((short) 2));
    }
}
