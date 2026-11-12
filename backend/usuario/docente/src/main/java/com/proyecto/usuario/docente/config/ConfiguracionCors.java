package com.proyecto.usuario.docente.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * T-307: CORS del módulo Docente.
 *
 * En local acepta el Vite de siempre. En despliegue se configura con la
 * variable de entorno APP_CORS_ORIGENES, con los orígenes separados por comas:
 *
 *   APP_CORS_ORIGENES=https://docente.example.com,https://portal.example.com
 *
 * Antes cada controlador tenía su propio @CrossOrigin con localhost fijo; al
 * pasar a una configuración central, el dominio desplegado no requiere tocar código.
 */
@Configuration
public class ConfiguracionCors implements WebMvcConfigurer {

    static final String ORIGENES_POR_DEFECTO = "http://localhost:5173";

    private final String origenes;

    public ConfiguracionCors(
            @Value("${app.cors.origenes:" + ORIGENES_POR_DEFECTO + "}") String origenes) {
        this.origenes = origenes;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenes())
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    // separa la lista, quita espacios y descarta entradas vacías
    String[] origenes() {
        return Arrays.stream(origenes.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toArray(String[]::new);
    }
}
