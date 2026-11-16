package com.proyecto.usuario.docente.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-199: la configuración de Swagger debe publicar los datos del módulo Docente.
 */
class OpenApiConfigTest {

    @Test
    void defineLaInformacionDelModuloDocente() {
        OpenAPI api = new OpenApiConfig().docenteOpenAPI();

        assertEquals("API Docente", api.getInfo().getTitle());
        assertEquals("1.0.0", api.getInfo().getVersion());
        assertTrue(api.getInfo().getDescription().contains("X-Docente-Id"));
    }
}
