package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-396 (US-15): la migración V20 agrega el puntaje de fluidez a la tabla
 * `evidencia`. Prueba de contrato: si alguien cambia la migración ya aplicada,
 * falla.
 */
class MigracionEvidenciaFluidezTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V20__evidencia_puntaje_fluidez.sql").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void agregaElPuntajeDeFluidezALaEvidencia() {
        assertTrue(SQL.contains("alter table evidencia"));
        assertTrue(SQL.contains("add column puntaje_fluidez smallint"));
    }

    @Test
    void elPuntajeVaDe0A100() {
        assertTrue(SQL.contains("check (puntaje_fluidez between 0 and 100)"));
    }

    @Test
    void esOpcionalPorqueSeCalculaDespues() {
        assertFalse(SQL.contains("not null"));
    }
}
