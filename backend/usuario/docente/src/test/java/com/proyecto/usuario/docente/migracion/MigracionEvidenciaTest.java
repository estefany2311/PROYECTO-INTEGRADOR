package com.proyecto.usuario.docente.migracion;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-296 (US-31): la migración V10 crea la tabla `evidencia` que comparten el
 * módulo docente y el de estudiante. Prueba de contrato: si alguien cambia la
 * migración ya aplicada, falla.
 */
class MigracionEvidenciaTest {

    private static final String SQL = leerMigracion();

    private static String leerMigracion() {
        try (InputStream in = new ClassPathResource("db/migration/V10__evidencia.sql").getInputStream()) {
            // se normaliza (minúsculas y espacios simples) para comparar sin depender del formato
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase()
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void creaLaTablaEvidenciaConAlumnoYNivel() {
        assertTrue(SQL.contains("create table evidencia"));
        assertTrue(SQL.contains("estudiante_id bigint not null"));
        assertTrue(SQL.contains("references estudiante (id)"));
        assertTrue(SQL.contains("nivel_id smallint not null"));
        assertTrue(SQL.contains("references nivel (id)"));
    }

    @Test
    void diferenciaLasEvidenciasDeAudioYDeTexto() {
        assertTrue(SQL.contains("tipo varchar(10) not null"));
        assertTrue(SQL.contains("check (tipo in ('audio', 'texto'))"));
        assertTrue(SQL.contains("audio_url varchar(500)"));
        assertTrue(SQL.contains("texto text"));
    }

    @Test
    void exigeElContenidoQueCorrespondeAlTipo() {
        assertTrue(SQL.contains("constraint chk_evidencia_contenido check"));
        assertTrue(SQL.contains("(tipo = 'audio' and audio_url is not null)"));
        assertTrue(SQL.contains("(tipo = 'texto' and texto is not null)"));
    }

    @Test
    void guardaElResultadoIaYElEstadoDeRevision() {
        assertTrue(SQL.contains("resultado_ia text"));
        assertTrue(SQL.contains("estado_revision varchar(15) not null default 'pendiente'"));
        assertTrue(SQL.contains("check (estado_revision in ('pendiente', 'aprobada', 'rechazada'))"));
        assertTrue(SQL.contains("revisado_en timestamp"));
    }

    @Test
    void borrarElAlumnoONivelLimpiaSusEvidencias() {
        assertTrue(SQL.contains("on delete cascade"));
    }

    @Test
    void tieneIndicesParaLasConsultasFrecuentes() {
        assertTrue(SQL.contains("create index idx_evidencia_estudiante on evidencia (estudiante_id)"));
        assertTrue(SQL.contains("create index idx_evidencia_revision on evidencia (estado_revision, creado_en desc)"));
    }
}
