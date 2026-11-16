package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.exception.SesionVencidaException;
import com.proyecto.usuario.estudiante.exception.TokenFaltanteException;
import com.proyecto.usuario.estudiante.repository.SesionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// T-243: el alumno se identifica por su token, nunca por la URL
@ExtendWith(MockitoExtension.class)
class AutenticadorTabletTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final GeneradorToken GENERADOR = new GeneradorToken();

    @Mock
    SesionRepository sesiones;

    AutenticadorTablet autenticador;

    @BeforeEach
    void setUp() {
        autenticador = new AutenticadorTablet(sesiones, GENERADOR, RELOJ);
    }

    @Test
    void tokenVigenteDevuelveElAlumno() {
        when(sesiones.buscarEstudianteVigente(GENERADOR.hash("token-de-ana"), LocalDateTime.now(RELOJ)))
                .thenReturn(Optional.of(1L));

        assertEquals(1L, autenticador.estudianteDe("Bearer token-de-ana"));
    }

    // Vencido, inventado o cerrado con "Salir"
    @Test
    void tokenQueYaNoSirveResponde401() {
        when(sesiones.buscarEstudianteVigente(any(), any())).thenReturn(Optional.empty());

        assertThrows(SesionVencidaException.class, () -> autenticador.estudianteDe("Bearer viejo"));
    }

    @Test
    void sinCabeceraOConOtroTipoDeCabeceraNoSeConsultaLaBase() {
        assertThrows(TokenFaltanteException.class, () -> autenticador.estudianteDe(null));
        assertThrows(TokenFaltanteException.class, () -> autenticador.estudianteDe("Basic YW5hOjEyMzQ="));
        assertThrows(TokenFaltanteException.class, () -> autenticador.estudianteDe("Bearer   "));

        verifyNoInteractions(sesiones);
    }
}
