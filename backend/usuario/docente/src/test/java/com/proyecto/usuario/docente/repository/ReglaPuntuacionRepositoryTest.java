package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.ReglaPuntuacion;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// la regla del admin (US-53) manda; si falta, rige 100 puntos y 60 / 80 / 95
class ReglaPuntuacionRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ReglaPuntuacionRepository repositorio = new ReglaPuntuacionRepository(jdbc);

    @SuppressWarnings("unchecked")
    @Test
    void usaLaReglaDelAdministrador() {
        ReglaPuntuacion delAdmin = new ReglaPuntuacion("lectura", 10, 50, 70, 90);
        when(jdbc.query(anyString(), any(RowMapper.class), eq("lectura"))).thenReturn(List.of(delAdmin));

        assertEquals(delAdmin, repositorio.porTipo("lectura"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void sinFilaUsaLaReglaPorDefecto() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("oralidad"))).thenReturn(List.of());

        assertEquals(new ReglaPuntuacion("oralidad", 100, 60, 80, 95), repositorio.porTipo("oralidad"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void siLaTablaNoExisteUsaLaReglaPorDefecto() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("escritura")))
                .thenThrow(new DataAccessResourceFailureException("la relación no existe"));

        assertEquals(ReglaPuntuacion.porDefecto("escritura"), repositorio.porTipo("escritura"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void siLaFilaNoEsValidaUsaLaReglaPorDefecto() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("lectura")))
                .thenThrow(new IllegalArgumentException("Los umbrales de estrellas deben ser crecientes"));

        assertEquals(ReglaPuntuacion.porDefecto("lectura"), repositorio.porTipo("lectura"));
    }

    @Test
    void losPuntosSonProporcionalesALaNotaYSeRedondean() {
        ReglaPuntuacion regla = new ReglaPuntuacion("lectura", 10, 50, 70, 90);

        assertEquals(0, regla.puntosPara(0));
        assertEquals(9, regla.puntosPara(85));
        assertEquals(10, regla.puntosPara(100));
        assertEquals(100, ReglaPuntuacion.porDefecto("lectura").puntosPara(100));
    }
}
