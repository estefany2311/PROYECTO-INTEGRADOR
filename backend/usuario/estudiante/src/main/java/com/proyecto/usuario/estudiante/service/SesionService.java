package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.SesionResponse;
import com.proyecto.usuario.estudiante.exception.PinIncorrectoException;
import com.proyecto.usuario.estudiante.model.Estudiante;
import com.proyecto.usuario.estudiante.repository.EstudianteRepository;
import com.proyecto.usuario.estudiante.repository.SesionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class SesionService {

    private final EstudianteRepository estudiantes;
    private final SesionRepository sesiones;
    private final PasswordEncoder codificadorPin;
    private final GeneradorToken generadorToken;
    private final Clock reloj;
    private final long horasSesion;

    public SesionService(EstudianteRepository estudiantes,
                         SesionRepository sesiones,
                         PasswordEncoder codificadorPin,
                         GeneradorToken generadorToken,
                         Clock reloj,
                         @Value("${estudiante.sesion.horas:8}") long horasSesion) {
        this.estudiantes = estudiantes;
        this.sesiones = sesiones;
        this.codificadorPin = codificadorPin;
        this.generadorToken = generadorToken;
        this.reloj = reloj;
        this.horasSesion = horasSesion;
    }

    /**
     * Valida el PIN del perfil elegido y crea una sesión nueva.
     * Si el estudiante no existe, está inactivo o no tiene PIN, responde igual que un PIN incorrecto,
     * para no dar pistas de qué cuentas existen.
     */
    public SesionResponse iniciarSesion(long estudianteId, String pin) {
        Estudiante estudiante = estudiantes.buscarPorId(estudianteId)
                .filter(Estudiante::activo)
                .filter(e -> e.pinHash() != null)
                .orElse(null);

        if (estudiante == null || !codificadorPin.matches(pin, estudiante.pinHash())) {
            throw new PinIncorrectoException();
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        String token = generadorToken.nuevoToken();
        sesiones.guardar(generadorToken.hash(token), estudiante.id(), ahora, ahora.plusHours(horasSesion));
        return new SesionResponse(token, estudiante.id(), estudiante.nombre(), estudiante.avatarUrl());
    }
}