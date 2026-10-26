package com.proyecto.usuario.docente.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ConfiguracionSeguridad {

    // Mismo algoritmo (BCrypt) con el que el módulo estudiante valida el PIN
    @Bean
    public PasswordEncoder codificadorPin() {
        return new BCryptPasswordEncoder();
    }
}