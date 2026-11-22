package com.proyecto.usuario.docente.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-199: la configuración de Swagger debe publicar los datos del módulo Docente.
 * T-307: además, servidor configurable, esquemas de seguridad y secciones.
 */
class OpenApiConfigTest {

    private final OpenAPI api = new OpenApiConfig().docenteOpenAPI();

    @Test
    void defineLaInformacionDelModuloDocente() {
        assertEquals("API Docente", api.getInfo().getTitle());
        assertEquals("1.0.0", api.getInfo().getVersion());
        assertTrue(api.getInfo().getDescription().contains("X-Docente-Id"));
    }

    @Test
    void publicaElServidorLocalPorDefecto() {
        assertEquals("http://localhost:8081", api.getServers().get(0).getUrl());
    }

    @Test
    void defineElHeaderDelDocenteYElTokenDelAlumno() {
        SecurityScheme docente = api.getComponents().getSecuritySchemes().get("docenteId");
        SecurityScheme alumno = api.getComponents().getSecuritySchemes().get("tokenEstudiante");

        assertNotNull(docente);
        assertEquals(SecurityScheme.Type.APIKEY, docente.getType());
        assertEquals(SecurityScheme.In.HEADER, docente.getIn());
        assertEquals("X-Docente-Id", docente.getName());

        assertNotNull(alumno);
        assertEquals(SecurityScheme.Type.HTTP, alumno.getType());
        assertEquals("bearer", alumno.getScheme());
    }

    @Test
    void ordenaLasSeccionesDelPortal() {
        assertEquals(11, api.getTags().size());
        assertEquals("Aulas", api.getTags().get(0).getName());
        assertEquals("Mapa del alumno", api.getTags().get(10).getName());
    }
}
