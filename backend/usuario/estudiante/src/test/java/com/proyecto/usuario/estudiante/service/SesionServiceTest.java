package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.SesionResponse;
import com.proyecto.usuario.estudiante.exception.PinIncorrectoException;
import com.proyecto.usuario.estudiante.model.Estudiante;
import com.proyecto.usuario.estudiante.repository.EstudianteRepository;
import com.proyecto.usuario.estudiante.repository.SesionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SesionServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-20T13:00:00Z"), ZoneId.of("America/Lima"));
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(4); // costo bajo: pruebas rápidas

    @Mock
    EstudianteRepository estudiantes;

    @Mock
    SesionRepository sesiones;

    SesionService service;

    @BeforeEach
    void setUp() {
        service = new SesionService(estudiantes, sesiones, BCRYPT, new GeneradorToken(), RELOJ, 8);
    }

    private Estudiante ana(boolean activo) {
        return new Estudiante(1, "Ana", "https://avatar/ana.png", BCRYPT.encode("1234"), activo);
    }

    // Con el PIN correcto se crea una sesión de 8 horas y se guarda solo el hash del token
    @Test
    void pinCorrectoDevuelveTokenYGuardaSoloElHash() {
        when(estudiantes.buscarPorId(1)).thenReturn(Optional.of(ana(true)));

        SesionResponse resp = service.iniciarSesion(1, "1234");

        assertNotNull(resp.token());
        assertEquals("Ana", resp.nombre());
        LocalDateTime ahora = LocalDateTime.now(RELOJ);
        verify(sesiones).guardar(argThat(hash -> hash.length() == 64 && !hash.equals(resp.token())),
                eq(1L), eq(ahora), eq(ahora.plusHours(8)));
    }

    @Test
    void pinIncorrectoLanzaExcepcionYNoCreaSesion() {
        when(estudiantes.buscarPorId(1)).thenReturn(Optional.of(ana(true)));

        assertThrows(PinIncorrectoException.class, () -> service.iniciarSesion(1, "9999"));
        verify(sesiones, never()).guardar(any(), anyLong(), any(), any());
    }

    // Un alumno desactivado por el docente no puede entrar aunque sepa su PIN
    @Test
    void estudianteInactivoNoPuedeEntrar() {
        when(estudiantes.buscarPorId(1)).thenReturn(Optional.of(ana(false)));

        assertThrows(PinIncorrectoException.class, () -> service.iniciarSesion(1, "1234"));
    }

    @Test
    void estudianteInexistenteRespondeIgualQuePinIncorrecto() {
        when(estudiantes.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(PinIncorrectoException.class, () -> service.iniciarSesion(99, "1234"));
    }
}