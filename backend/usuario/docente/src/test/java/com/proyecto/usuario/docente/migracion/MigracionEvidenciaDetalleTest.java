package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-371 (US-32): la migración V13 agrega la transcripción y el puntaje de la IA
 * a la tabla `evidencia`. Prueba de contrato: si alguien cambia la migración ya
 * aplicada, falla.
 */
class MigracionEvidenciaDetalleTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V13__evidencia_transcripcion_y_puntaje_ia.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void agregaLaTranscripcionYElPuntajeAlaEvidencia() {
        assertTrue(SQL.contains("alter table evidencia"));
        assertTrue(SQL.contains("add column transcripcion text"));
        assertTrue(SQL.contains("add column puntaje_ia smallint"));
    }

    @Test
    void elPuntajeDeLaIaVaDeCeroACien() {
        assertTrue(SQL.contains("constraint chk_evidencia_puntaje_ia check (puntaje_ia between 0 and 100)"));
    }

    @Test
    void completaLasEvidenciasDePruebaSinQuemarIds() {
        assertTrue(SQL.contains("where audio_url ="));
        assertTrue(SQL.contains("where texto ="));
        assertTrue(SQL.contains("puntaje_ia = 85"));
        assertTrue(SQL.contains("puntaje_ia = 30"));
    }
}
