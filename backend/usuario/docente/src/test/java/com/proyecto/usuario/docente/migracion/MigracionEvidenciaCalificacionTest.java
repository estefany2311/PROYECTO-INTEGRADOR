package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-377 (US-33): la migración V15 agrega la calificación final del docente y
 * el estado `corregida` a la tabla `evidencia`. Prueba de contrato: si alguien
 * cambia la migración ya aplicada, falla.
 */
class MigracionEvidenciaCalificacionTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V15__evidencia_calificacion_final.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void agregaLaCalificacionFinalDelDocente() {
        assertTrue(SQL.contains("alter table evidencia"));
        assertTrue(SQL.contains("add column calificacion_final smallint"));
    }

    @Test
    void limitaLaCalificacionFinalAlRangoDeCeroACien() {
        assertTrue(SQL.contains("chk_evidencia_calificacion_final"));
        assertTrue(SQL.contains("check (calificacion_final between 0 and 100)"));
    }

    @Test
    void admiteElEstadoCorregidaSinPerderLosEstadosAnteriores() {
        // el CHECK de V10 se reemplaza para que `corregida` sea válido
        assertTrue(SQL.contains("drop constraint"));
        assertTrue(SQL.contains(
                "check (estado_revision in ('pendiente', 'aprobada', 'rechazada', 'corregida'))"));
    }
}
