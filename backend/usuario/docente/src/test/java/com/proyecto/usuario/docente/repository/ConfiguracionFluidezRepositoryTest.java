package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.ConfiguracionFluidez;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// T-398 (US-15): la configuración de la US-51 manda; si falta, rige la de por defecto
class ConfiguracionFluidezRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ConfiguracionFluidezRepository repositorio = new ConfiguracionFluidezRepository(jdbc);

    @SuppressWarnings("unchecked")
    @Test
    void usaLaConfiguracionDelAdministradorSiExiste() {
        ConfiguracionFluidez delAdmin = new ConfiguracionFluidez(4, 80, 1.8);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(4))).thenReturn(List.of(delAdmin));

        assertEquals(delAdmin, repositorio.porGrado(4));
    }

    @SuppressWarnings("unchecked")
    @Test
    void sinFilaParaElGradoUsaLosValoresIniciales() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(2))).thenReturn(List.of());

        assertEquals(new ConfiguracionFluidez(2, 45, 2.5), repositorio.porGrado(2));
    }

    @SuppressWarnings("unchecked")
    @Test
    void siLaTablaNoExisteUsaLosValoresIniciales() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(6)))
                .thenThrow(new DataAccessResourceFailureException("la relación no existe"));

        assertEquals(new ConfiguracionFluidez(6, 95, 1.5), repositorio.porGrado(6));
    }

    @Test
    void losValoresInicialesCoincidenConLosDelModuloAdmin() {
        assertEquals(30, ConfiguracionFluidez.porDefecto(1).palabrasPorMinuto());
        assertEquals(3.0, ConfiguracionFluidez.porDefecto(1).toleranciaPausas());
        assertEquals(60, ConfiguracionFluidez.porDefecto(3).palabrasPorMinuto());
        assertEquals(75, ConfiguracionFluidez.porDefecto(4).palabrasPorMinuto());
        assertEquals(1.5, ConfiguracionFluidez.porDefecto(5).toleranciaPausas());
    }

    @Test
    void rechazaValoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionFluidez(3, 0, 2.0));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionFluidez(3, 60, -1));
    }
}
