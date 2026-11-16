package com.proyecto.usuario.estudiante.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
public class ConfiguracionGeneral {

    // Reloj inyectable: en las pruebas se reemplaza por uno fijo
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }

    // El PIN se guarda cifrado con BCrypt; aquí solo se compara
    @Bean
    public PasswordEncoder codificadorPin() {
        return new BCryptPasswordEncoder();
    }
}