package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.ReglasLogin;
import com.proyecto.usuario.estudiante.dto.SesionResponse;
import com.proyecto.usuario.estudiante.exception.CuentaBloqueadaException;
import com.proyecto.usuario.estudiante.exception.PinIncorrectoException;
import com.proyecto.usuario.estudiante.model.Estudiante;
import com.proyecto.usuario.estudiante.repository.EstudianteRepository;
import com.proyecto.usuario.estudiante.repository.IntentoLoginRepository;
import com.proyecto.usuario.estudiante.repository.SesionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SesionService {

    private final EstudianteRepository estudiantes;
    private final SesionRepository sesiones;
    private final IntentoLoginRepository intentos;
    private final PasswordEncoder codificadorPin;
    private final GeneradorToken generadorToken;
    private final Clock reloj;
    private final ReglasLogin reglas;

    public SesionService(EstudianteRepository estudiantes,
                         SesionRepository sesiones,
                         IntentoLoginRepository intentos,
                         PasswordEncoder codificadorPin,
                         GeneradorToken generadorToken,
                         Clock reloj,
                         ReglasLogin reglas) {
        this.estudiantes = estudiantes;
        this.sesiones = sesiones;
        this.intentos = intentos;
        this.codificadorPin = codificadorPin;
        this.generadorToken = generadorToken;
        this.reloj = reloj;
        this.reglas = reglas;
    }

    /**
     * Valida el PIN del perfil elegido y crea una sesión nueva.
     * Si el estudiante no existe, está inactivo o no tiene PIN, responde igual que un PIN incorrecto
     * para no dar pistas de qué cuentas existen.
     */
    public SesionResponse iniciarSesion(long estudianteId, String pin) {
        LocalDateTime ahora = LocalDateTime.now(reloj);

        // T-203: mientras dure el bloqueo no se revisa el PIN, ni siquiera si es el correcto
        Optional<LocalDateTime> bloqueo = intentos.bloqueadoHasta(estudianteId);
        if (bloqueo.isPresent() && bloqueo.get().isAfter(ahora)) {
            throw new CuentaBloqueadaException(segundosEntre(ahora, bloqueo.get()));
        }

        Estudiante estudiante = estudiantes.buscarPorId(estudianteId)
                .filter(Estudiante::activo)
                .filter(e -> e.pinHash() != null)
                .orElse(null);

        if (estudiante == null || !codificadorPin.matches(pin, estudiante.pinHash())) {
            throw registrarFallo(estudianteId, ahora);
        }

        intentos.limpiar(estudianteId);
        String token = generadorToken.nuevoToken();
        sesiones.guardar(generadorToken.hash(token), estudiante.id(), ahora, ahora.plusHours(reglas.horasSesion()));
        return new SesionResponse(token, estudiante.id(), estudiante.nombre(), estudiante.avatarUrl());
    }

    // Devuelve el error a lanzar: PIN incorrecto con intentos restantes, o bloqueo si ya no quedan
    private RuntimeException registrarFallo(long estudianteId, LocalDateTime ahora) {
        int fallidos = intentos.registrarFallo(estudianteId);
        if (fallidos >= reglas.maxIntentos()) {
            LocalDateTime hasta = ahora.plusMinutes(reglas.minutosBloqueo());
            intentos.bloquear(estudianteId, hasta);
            return new CuentaBloqueadaException(segundosEntre(ahora, hasta));
        }
        return new PinIncorrectoException(reglas.maxIntentos() - fallidos);
    }

    private static long segundosEntre(LocalDateTime desde, LocalDateTime hasta) {
        return Math.max(1, Duration.between(desde, hasta).toSeconds());
    }
}
