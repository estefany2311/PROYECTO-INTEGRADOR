package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.ReglasLogin;
import com.proyecto.usuario.estudiante.dto.SesionResponse;
import com.proyecto.usuario.estudiante.exception.CuentaBloqueadaException;
import com.proyecto.usuario.estudiante.exception.PinIncorrectoException;
import com.proyecto.usuario.estudiante.model.Estudiante;
import com.proyecto.usuario.estudiante.repository.EstudianteRepository;
import com.proyecto.usuario.estudiante.repository.IntentoLoginRepository;
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
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(4); // costo bajo: pruebas rápidas
    private static final ReglasLogin REGLAS = new ReglasLogin(5, 2, 8);

    @Mock
    EstudianteRepository estudiantes;

    @Mock
    SesionRepository sesiones;

    @Mock
    IntentoLoginRepository intentos;

    SesionService service;

    @BeforeEach
    void setUp() {
        service = new SesionService(estudiantes, sesiones, intentos, BCRYPT, new GeneradorToken(), RELOJ, REGLAS);
    }

    private Estudiante ana(boolean activo) {
        return new Estudiante(1, "Ana", "https://avatar/ana.png", BCRYPT.encode("1234"), activo);
    }

    // Con el PIN correcto se crea una sesión de 8 horas, se guarda solo el hash del token y se borran los fallos
    @Test
    void pinCorrectoDevuelveTokenYGuardaSoloElHash() {
        when(estudiantes.buscarPorId(1)).thenReturn(Optional.of(ana(true)));

        SesionResponse resp = service.iniciarSesion(1, "1234");

        assertNotNull(resp.token());
        assertEquals("Ana", resp.nombre());
        verify(sesiones).guardar(argThat(hash -> hash.length() == 64 && !hash.equals(resp.token())),
                eq(1L), eq(AHORA), eq(AHORA.plusHours(8)));
        verify(intentos).limpiar(1);
    }

    @Test
    void pinIncorrectoLanzaExcepcionConIntentosRestantesYNoCreaSesion() {
        when(estudiantes.buscarPorId(1)).thenReturn(Optional.of(ana(true)));
        when(intentos.registrarFallo(1)).thenReturn(2);

        PinIncorrectoException e = assertThrows(PinIncorrectoException.class, () -> service.iniciarSesion(1, "9999"));

        assertEquals(3, e.getIntentosRestantes());
        verify(sesiones, never()).guardar(any(), anyLong(), any(), any());
        verify(intentos, never()).bloquear(anyLong(), any());
    }

    // Un alumno desactivado por el docente no puede entrar aunque sepa su PIN
    @Test
    void estudianteInactivoNoPuedeEntrar() {
        when(estudiantes.buscarPorId(1)).thenReturn(Optional.of(ana(false)));
        when(intentos.registrarFallo(1)).thenReturn(1);

        assertThrows(PinIncorrectoException.class, () -> service.iniciarSesion(1, "1234"));
    }

    @Test
    void estudianteInexistenteRespondeIgualQuePinIncorrecto() {
        when(estudiantes.buscarPorId(99)).thenReturn(Optional.empty());
        when(intentos.registrarFallo(99)).thenReturn(1);

        assertThrows(PinIncorrectoException.class, () -> service.iniciarSesion(99, "1234"));
    }

    // T-203: al quinto fallo seguido la cuenta queda bloqueada 2 minutos
    @Test
    void quintoFalloBloqueaLaCuenta() {
        when(estudiantes.buscarPorId(1)).thenReturn(Optional.of(ana(true)));
        when(intentos.registrarFallo(1)).thenReturn(5);

        CuentaBloqueadaException e = assertThrows(CuentaBloqueadaException.class, () -> service.iniciarSesion(1, "9999"));

        assertEquals(120, e.getSegundosRestantes());
        verify(intentos).bloquear(1, AHORA.plusMinutes(2));
    }

    // Durante el bloqueo ni siquiera el PIN correcto deja entrar, y no se cuenta como otro fallo
    @Test
    void cuentaBloqueadaRechazaInclusoElPinCorrecto() {
        when(intentos.bloqueadoHasta(1)).thenReturn(Optional.of(AHORA.plusSeconds(45)));

        CuentaBloqueadaException e = assertThrows(CuentaBloqueadaException.class, () -> service.iniciarSesion(1, "1234"));

        assertEquals(45, e.getSegundosRestantes());
        verifyNoInteractions(estudiantes, sesiones);
        verify(intentos, never()).registrarFallo(anyLong());
    }

    @Test
    void bloqueoVencidoPermiteEntrar() {
        when(intentos.bloqueadoHasta(1)).thenReturn(Optional.of(AHORA.minusSeconds(1)));
        when(estudiantes.buscarPorId(1)).thenReturn(Optional.of(ana(true)));

        SesionResponse resp = service.iniciarSesion(1, "1234");

        assertEquals(1, resp.estudianteId());
        verify(intentos).limpiar(1);
    }
}
