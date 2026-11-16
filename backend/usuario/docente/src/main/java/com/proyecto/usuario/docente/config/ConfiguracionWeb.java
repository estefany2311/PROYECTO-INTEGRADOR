package com.proyecto.usuario.docente.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final InterceptorTokenEstudiante interceptorTokenEstudiante;

    public ConfiguracionWeb(InterceptorTokenEstudiante interceptorTokenEstudiante) {
        this.interceptorTokenEstudiante = interceptorTokenEstudiante;
    }

    // Solo las rutas que usa la tablet del alumno; las del docente no cambian
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptorTokenEstudiante)
                .addPathPatterns("/api/estudiante/mapa");
    }
}
