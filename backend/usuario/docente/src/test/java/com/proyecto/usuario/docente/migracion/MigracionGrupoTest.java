package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-194: la migración V8 crea las tablas `grupo` y `grupo_alumno`.
 * Prueba de contrato: si alguien cambia la migración ya aplicada, falla.
 */
class MigracionGrupoTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource("db/migration/V8__grupo_y_grupo_alumno.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void creaLaTablaGrupoConSuAula() {
        assertTrue(SQL.contains("create table grupo"));
        assertTrue(SQL.contains("aula_id bigint not null"));
        assertTrue(SQL.contains("references aula (id)"));
        assertTrue(SQL.contains("nombre varchar(60) not null"));
    }

    @Test
    void noPermiteDosGruposConElMismoNombreEnElAula() {
        assertTrue(SQL.contains("unique (aula_id, nombre)"));
    }

    @Test
    void creaLaTablaIntermediaGrupoAlumno() {
        assertTrue(SQL.contains("create table grupo_alumno"));
        assertTrue(SQL.contains("grupo_id bigint not null"));
        assertTrue(SQL.contains("estudiante_id bigint not null"));
        assertTrue(SQL.contains("references grupo (id)"));
        assertTrue(SQL.contains("references estudiante (id)"));
        // la clave compuesta impide repetir al mismo alumno en el grupo
        assertTrue(SQL.contains("primary key (grupo_id, estudiante_id)"));
    }

    @Test
    void borrarElGrupoOElAlumnoLimpiaLaTablaIntermedia() {
        assertTrue(SQL.contains("on delete cascade"));
    }

    @Test
    void tieneIndicesParaLasConsultasFrecuentes() {
        assertTrue(SQL.contains("create index idx_grupo_aula on grupo (aula_id)"));
        assertTrue(SQL.contains("create index idx_grupo_alumno_estudiante on grupo_alumno (estudiante_id)"));
    }
}
