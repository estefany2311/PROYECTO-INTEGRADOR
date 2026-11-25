package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La migración V21 crea la tabla del mejor intento por alumno y nivel. Prueba de
 * contrato: si alguien cambia la migración ya aplicada, falla.
 */
class MigracionPuntajeNivelTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource(
                "db/migration/V21__puntaje_nivel_estudiante.sql").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void creaLaTablaConLaNotaLosPuntosYLasEstrellas() {
        assertTrue(SQL.contains("create table puntaje_nivel_estudiante"));
        assertTrue(SQL.contains("nota smallint not null check (nota between 0 and 100)"));
        assertTrue(SQL.contains("puntos int not null default 0"));
        assertTrue(SQL.contains("estrellas int not null default 0 check (estrellas between 0 and 3)"));
    }

    @Test
    void haySoloUnaFilaPorAlumnoYNivel() {
        assertTrue(SQL.contains("unique (estudiante_id, nivel_id)"));
    }

    @Test
    void borrarElAlumnoOElNivelBorraSuMejorIntento() {
        assertTrue(SQL.contains("references estudiante (id) on delete cascade"));
        assertTrue(SQL.contains("references nivel (id) on delete cascade"));
    }
}
