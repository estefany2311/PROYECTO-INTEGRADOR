package com.proyecto.usuario.docente.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * T-199: documentación OpenAPI del módulo Docente.
 * T-307: agrega servidor configurable, esquemas de seguridad y el orden de las
 * secciones, para que Swagger UI sirva también en el despliegue.
 *
 * Swagger UI: http://localhost:8081/swagger-ui.html
 * JSON:       http://localhost:8081/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    // T-307: en despliegue se sobreescribe con APP_OPENAPI_SERVIDOR.
    @Value("${app.openapi.servidor:http://localhost:8081}")
    private String servidor = "http://localhost:8081";

    @Bean
    public OpenAPI docenteOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Docente")
                        .version("1.0.0")
                        .description("""
                                Endpoints del portal docente: aulas, alumnos, catálogo,
                                asignaciones, grupos, avatares, PIN, dashboard, detalle,
                                evidencias y el mapa de la tablet.

                                Las rutas /api/docente requieren el header X-Docente-Id.
                                GET /api/estudiante/mapa requiere Authorization: Bearer <token>.
                                GET /api/docente/grados y /api/docente/avatares no piden header.
                                """))
                .servers(List.of(new Server().url(servidor).description("Módulo Docente")))
                .components(new Components()
                        .addSecuritySchemes("docenteId", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Docente-Id")
                                .description("Id del docente autenticado."))
                        .addSecuritySchemes("tokenEstudiante", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("Token que entrega el acceso de la tablet del alumno.")))
                .tags(List.of(
                        new Tag().name("Aulas").description("Registro y listado de aulas multigrado."),
                        new Tag().name("Alumnos").description("Registro, edición y estado de los alumnos."),
                        new Tag().name("Acceso del alumno").description("Avatar y PIN de la tablet."),
                        new Tag().name("Catálogo").description("Lecturas activas de los grados del aula."),
                        new Tag().name("Asignaciones").description("Lectura asignada a un alumno."),
                        new Tag().name("Grupos").description("Grupos y asignación a todo el grupo."),
                        new Tag().name("Avatares").description("Catálogo de avatares."),
                        new Tag().name("Dashboard").description("Rendimiento del aula por semáforo."),
                        new Tag().name("Detalle del alumno").description("Progreso y evaluaciones del alumno."),
                        new Tag().name("Evidencias").description("Evidencias pendientes de revisión."),
                        new Tag().name("Mapa del alumno").description("Mapa de niveles de la tablet.")));
    }
}
