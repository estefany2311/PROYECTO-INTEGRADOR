package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-375 (US-32): la migración V14 agrega las palabras que la IA marcó en la
 * evidencia. Prueba de contrato: si alguien cambia la migración ya aplicada,
 * falla.
 */
class MigracionEvidenciaPalabrasTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V14__evidencia_palabras_marcadas.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void agregaLasPalabrasMarcadasAlaEvidencia() {
        assertTrue(SQL.contains("alter table evidencia"));
        assertTrue(SQL.contains("add column palabras_marcadas text"));
    }

    @Test
    void marcaPalabrasEnLasEvidenciasDePruebaSinQuemarIds() {
        assertTrue(SQL.contains("where audio_url ="));
        assertTrue(SQL.contains("palabras_marcadas = 'brilla,juegan'"));
        assertTrue(SQL.contains("palabras_marcadas = 'abrazo,días'"));
        assertTrue(SQL.contains("palabras_marcadas = 'vaca,pasto'"));
    }
}
