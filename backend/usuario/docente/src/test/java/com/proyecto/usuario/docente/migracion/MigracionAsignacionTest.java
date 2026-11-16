package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-191: la migración V6 crea la tabla `asignacion` con lectura, alumno y fecha.
 * Es una prueba de contrato: si alguien cambia la migración ya aplicada, falla.
 */
class MigracionAsignacionTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource("db/migration/V6__asignacion.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void creaLaTablaConLecturaAlumnoYFecha() {
        assertTrue(SQL.contains("create table asignacion"));
        assertTrue(SQL.contains("lectura_id bigint not null"));
        assertTrue(SQL.contains("estudiante_id bigint not null"));
        assertTrue(SQL.contains("fecha_asignacion timestamp not null default now()"));
    }

    @Test
    void apuntaALasTablasLecturasYEstudiante() {
        assertTrue(SQL.contains("references lecturas (id)"));
        assertTrue(SQL.contains("references estudiante (id)"));
    }

    @Test
    void borrarUnAlumnoBorraSusAsignaciones() {
        assertTrue(SQL.contains("on delete cascade"));
    }

    @Test
    void evitaAsignarLaMismaLecturaDosVecesAlMismoAlumno() {
        assertTrue(SQL.contains("create unique index"));
        assertTrue(SQL.contains("(lectura_id, estudiante_id)"));
    }

    @Test
    void tieneIndiceParaBuscarLasTareasDeUnAlumno() {
        assertTrue(SQL.contains("create index idx_asignacion_estudiante on asignacion (estudiante_id)"));
    }
}
