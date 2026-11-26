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
 * Login docente: la migración V22 crea la tabla de sesiones. Prueba de
 * contrato: si alguien cambia la migración ya aplicada, falla.
 */
class MigracionSesionDocenteTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V22__sesion_docente.sql").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^--.*$", "")
                    .toLowerCase();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void guardaHuellasUnicasYSusVencimientos() {
        assertTrue(SQL.contains("create table sesion_docente"));
        assertTrue(SQL.contains("acceso_hash"));
        assertTrue(SQL.contains("refresh_hash"));
        assertTrue(SQL.contains("acceso_expira"));
        assertTrue(SQL.contains("refresh_expira"));
        assertTrue(SQL.contains("unique"));
    }

    @Test
    void noModificaNiEnlazaLasTablasDeAdmin() {
        assertFalse(SQL.contains("references"));
        assertFalse(SQL.contains("alter table"));
    }
}
