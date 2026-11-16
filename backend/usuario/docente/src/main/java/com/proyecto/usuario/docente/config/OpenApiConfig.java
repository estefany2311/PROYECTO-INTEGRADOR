package com.proyecto.usuario.docente.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * T-199: documentación OpenAPI del módulo Docente.
 *
 * Swagger UI: http://localhost:8081/swagger-ui.html
 * JSON:       http://localhost:8081/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI docenteOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API Docente")
                .version("1.0.0")
                .description("""
                        Endpoints del portal docente: aulas, alumnos, catálogo,
                        asignaciones, grupos, avatares y PIN.

                        Las rutas /api/docente requieren el header X-Docente-Id.
                        """));
    }
}
