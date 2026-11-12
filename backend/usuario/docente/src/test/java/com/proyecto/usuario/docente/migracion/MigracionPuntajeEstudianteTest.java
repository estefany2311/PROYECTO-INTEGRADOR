package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-301 (US-09): la migración V12 crea la tabla `puntaje_estudiante` con los
 * puntos y las estrellas acumulados de cada alumno. Prueba de contrato: si
 * alguien cambia la migración ya aplicada, falla.
 */
class MigracionPuntajeEstudianteTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V12__puntaje_estudiante.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void creaLaTablaPuntajeEstudiante() {
        assertTrue(SQL.contains("create table puntaje_estudiante"));
        assertTrue(SQL.contains("id bigserial primary key"));
    }

    @Test
    void guardaUnaSolaFilaPorAlumno() {
        assertTrue(SQL.contains("estudiante_id bigint not null unique"));
        assertTrue(SQL.contains("references estudiante (id)"));
    }

    @Test
    void acumulaPuntosYEstrellasDesdeCero() {
        assertTrue(SQL.contains("puntos int not null default 0"));
        assertTrue(SQL.contains("estrellas int not null default 0"));
    }

    @Test
    void impideAcumuladosNegativos() {
        assertTrue(SQL.contains("check (puntos >= 0)"));
        assertTrue(SQL.contains("check (estrellas >= 0)"));
    }

    @Test
    void registraLaUltimaActualizacion() {
        assertTrue(SQL.contains("actualizado_en timestamp not null default now()"));
    }

    @Test
    void borrarElAlumnoLimpiaSuPuntaje() {
        assertTrue(SQL.contains("on delete cascade"));
    }
}
