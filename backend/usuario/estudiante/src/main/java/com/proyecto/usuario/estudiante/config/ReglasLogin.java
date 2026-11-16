package com.proyecto.usuario.estudiante.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Valores configurables en application.properties (estudiante.login.* y estudiante.sesion.horas)
@Component
public class ReglasLogin {

    private final int maxIntentos;
    private final long minutosBloqueo;
    private final long horasSesion;

    public ReglasLogin(@Value("${estudiante.login.max-intentos:5}") int maxIntentos,
                       @Value("${estudiante.login.minutos-bloqueo:2}") long minutosBloqueo,
                       @Value("${estudiante.sesion.horas:8}") long horasSesion) {
        this.maxIntentos = maxIntentos;
        this.minutosBloqueo = minutosBloqueo;
        this.horasSesion = horasSesion;
    }

    public int maxIntentos() {
        return maxIntentos;
    }

    public long minutosBloqueo() {
        return minutosBloqueo;
    }

    public long horasSesion() {
        return horasSesion;
    }
}
