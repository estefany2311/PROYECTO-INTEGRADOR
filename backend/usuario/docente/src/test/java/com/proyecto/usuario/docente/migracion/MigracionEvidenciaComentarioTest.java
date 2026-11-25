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
 * T-383 (US-34): la migración V18 agrega el comentario de retroalimentación a
 * la tabla `evidencia`. Prueba de contrato: si alguien cambia la migración ya
 * aplicada, falla.
 */
class MigracionEvidenciaComentarioTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V18__evidencia_comentario.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void agregaElComentarioALaEvidencia() {
        assertTrue(SQL.contains("alter table evidencia"));
        assertTrue(SQL.contains("add column comentario text"));
    }

    @Test
    void elComentarioEsOpcional() {
        assertFalse(SQL.contains("not null"));
    }
}
