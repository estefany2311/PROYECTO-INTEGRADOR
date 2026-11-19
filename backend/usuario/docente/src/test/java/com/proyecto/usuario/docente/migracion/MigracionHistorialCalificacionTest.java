package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-380 (US-33): la migración V17 crea el historial de calificaciones con la
 * nota de la IA, la nota del docente, el motivo y la acción. Prueba de contrato:
 * si alguien cambia la migración ya aplicada, falla.
 */
class MigracionHistorialCalificacionTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V17__historial_calificacion.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void creaLaTablaDelHistorial() {
        assertTrue(SQL.contains("create table historial_calificacion"));
        assertTrue(SQL.contains("evidencia_id bigint not null"));
        assertTrue(SQL.contains("docente_id bigint not null"));
        assertTrue(SQL.contains("nota_ia smallint"));
        assertTrue(SQL.contains("nota_docente smallint not null"));
        assertTrue(SQL.contains("motivo text"));
        assertTrue(SQL.contains("creado_en timestamp not null default now()"));
    }

    @Test
    void borraElHistorialConLaEvidencia() {
        assertTrue(SQL.contains("references evidencia (id) on delete cascade"));
    }

    @Test
    void limitaLasNotasYLasAcciones() {
        assertTrue(SQL.contains("check (nota_ia between 0 and 100)"));
        assertTrue(SQL.contains("check (nota_docente between 0 and 100)"));
        assertTrue(SQL.contains("check (accion in ('aprobada', 'corregida'))"));
    }

    @Test
    void indexaLaConsultaPorEvidencia() {
        assertTrue(SQL.contains("idx_historial_calificacion_evidencia"));
        assertTrue(SQL.contains("on historial_calificacion (evidencia_id, creado_en desc)"));
    }
}
