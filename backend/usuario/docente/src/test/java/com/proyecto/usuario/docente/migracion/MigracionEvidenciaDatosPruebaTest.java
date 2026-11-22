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
 * T-297 (US-31): la migración V11 carga evidencias de audio y texto para el
 * aula de prueba. Prueba de contrato: si alguien cambia los datos ya cargados,
 * falla.
 */
class MigracionEvidenciaDatosPruebaTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource("db/migration/V11__evidencia_datos_prueba.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void cargaEvidenciasDeAudioYDeTexto() {
        assertTrue(SQL.contains("insert into evidencia"));
        assertTrue(SQL.contains("'audio'"));
        assertTrue(SQL.contains("'texto'"));
        assertTrue(SQL.contains("audio_url"));
        assertTrue(SQL.contains("texto,"));
    }

    @Test
    void usaElAulaYLosTresAlumnosDePrueba() {
        assertTrue(SQL.contains("where a.nombre = 'noemi de la cruz'"));
        assertTrue(SQL.contains("'minji prueba', 'delacruz'"));
        assertTrue(SQL.contains("'breysi', 'zavala'"));
        assertTrue(SQL.contains("'ester', 'zavala'"));
    }

    @Test
    void cubreLosTresEstadosDeRevision() {
        assertTrue(SQL.contains("'pendiente'"));
        assertTrue(SQL.contains("'aprobada'"));
        assertTrue(SQL.contains("'rechazada'"));
    }

    @Test
    void lasEvidenciasPendientesNoTienenFechaDeRevision() {
        assertTrue(SQL.contains(
                "case when v.estado_revision = 'pendiente' then null"));
    }

    @Test
    void tomaElNivelPorSuOrdenYNoQuemaIds() {
        assertTrue(SQL.contains("join nivel n on n.orden = v.nivel_orden"));
        // los ids de alumno y nivel los resuelve la consulta, no el script
        assertFalse(SQL.contains("values (1,"));
        assertFalse(SQL.contains("estudiante_id values"));
    }
}
