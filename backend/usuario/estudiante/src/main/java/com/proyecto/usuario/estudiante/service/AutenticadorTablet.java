package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.exception.SesionVencidaException;
import com.proyecto.usuario.estudiante.exception.TokenFaltanteException;
import com.proyecto.usuario.estudiante.repository.SesionRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * T-214: averigua qué alumno hace la petición a partir de "Authorization: Bearer <token>".
 * El id del alumno sale siempre del token, nunca de la URL: así nadie ve los datos de otro niño.
 */
@Component
public class AutenticadorTablet {

    private static final String PREFIJO = "Bearer ";

    private final SesionRepository sesiones;
    private final GeneradorToken generadorToken;
    private final Clock reloj;

    public AutenticadorTablet(SesionRepository sesiones, GeneradorToken generadorToken, Clock reloj) {
        this.sesiones = sesiones;
        this.generadorToken = generadorToken;
        this.reloj = reloj;
    }

    // 401 si falta la cabecera, o si el token no existe, venció o se cerró con "Salir"
    public long estudianteDe(String authorization) {
        if (authorization == null || !authorization.startsWith(PREFIJO)) {
            throw new TokenFaltanteException();
        }
        String token = authorization.substring(PREFIJO.length()).trim();
        if (token.isEmpty()) {
            throw new TokenFaltanteException();
        }
        return sesiones.buscarEstudianteVigente(generadorToken.hash(token), LocalDateTime.now(reloj))
                .orElseThrow(SesionVencidaException::new);
    }
}
