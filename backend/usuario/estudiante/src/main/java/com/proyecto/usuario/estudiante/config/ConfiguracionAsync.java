package com.proyecto.usuario.estudiante.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// T-416: Whisper usa mucho CPU, así que las lecturas se transcriben de una en una, en fila
@Configuration
@EnableAsync
public class ConfiguracionAsync {

    public static final String PROCESADOR_VOZ = "procesadorVoz";

    @Bean(name = PROCESADOR_VOZ)
    public ThreadPoolTaskExecutor procesadorVoz() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(1);
        ejecutor.setMaxPoolSize(1);
        ejecutor.setQueueCapacity(200);
        ejecutor.setThreadNamePrefix("voz-");
        ejecutor.initialize();
        return ejecutor;
    }
}
