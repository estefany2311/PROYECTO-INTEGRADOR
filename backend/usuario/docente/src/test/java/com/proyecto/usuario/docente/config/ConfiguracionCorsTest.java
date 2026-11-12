package com.proyecto.usuario.docente.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-307: el CORS deja de estar fijo en localhost y se configura por variable
 * de entorno para poder desplegar.
 */
class ConfiguracionCorsTest {

    @Test
    void porDefectoAceptaElViteLocal() {
        ConfiguracionCors cors = new ConfiguracionCors(
                ConfiguracionCors.ORIGENES_POR_DEFECTO);

        assertEquals(List.of("http://localhost:5173"), List.of(cors.origenes()));
    }

    @Test
    void separaVariosOrigenesYQuitaEspacios() {
        ConfiguracionCors cors = new ConfiguracionCors(
                "https://docente.example.com, https://portal.example.com");

        assertEquals(List.of("https://docente.example.com", "https://portal.example.com"),
                List.of(cors.origenes()));
    }

    @Test
    void descartaEntradasVacias() {
        ConfiguracionCors cors = new ConfiguracionCors(
                " https://docente.example.com , , ");

        assertEquals(List.of("https://docente.example.com"), List.of(cors.origenes()));
    }

    @Test
    void unaListaVaciaNoDejaOrigenes() {
        assertTrue(new ConfiguracionCors(" , ").origenes().length == 0);
    }
}
