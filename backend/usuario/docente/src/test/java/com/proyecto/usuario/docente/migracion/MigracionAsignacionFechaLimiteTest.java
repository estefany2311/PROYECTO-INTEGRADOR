package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-388 (US-38): la migración V19 agrega la fecha límite y el estado a la tabla
 * `asignacion`. Prueba de contrato: si alguien cambia la migración ya aplicada,
 * falla.
 */
class MigracionAsignacionFechaLimiteTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V19__asignacion_fecha_limite_y_estado.sql").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void agregaLaFechaLimiteOpcional() {
        assertTrue(SQL.contains("alter table asignacion"));
        assertTrue(SQL.contains("add column fecha_limite date,"));
    }

    @Test
    void elEstadoGuardadoEsPendienteOEntregadaYArrancaPendiente() {
        assertTrue(SQL.contains("add column estado varchar(10) not null default 'pendiente'"));
        assertTrue(SQL.contains("check (estado in ('pendiente', 'entregada'))"));
    }

    @Test
    void vencidaNoSeGuarda() {
        assertTrue(!SQL.contains("'vencida'"));
    }

    @Test
    void soloUnaTareaEntregadaTieneFechaDeEntrega() {
        assertTrue(SQL.contains("add column entregada_en timestamp"));
        assertTrue(SQL.contains("estado = 'entregada' and entregada_en is not null"));
        assertTrue(SQL.contains("estado = 'pendiente' and entregada_en is null"));
    }

    @Test
    void tieneIndiceParaLaFechaLimite() {
        assertTrue(SQL.contains("create index idx_asignacion_fecha_limite on asignacion (fecha_limite)"));
    }
}
