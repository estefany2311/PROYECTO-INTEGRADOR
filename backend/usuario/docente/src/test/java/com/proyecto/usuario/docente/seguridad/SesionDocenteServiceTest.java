package com.proyecto.usuario.docente.seguridad;

import com.proyecto.usuario.docente.dto.SesionDocenteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// login docente: solo entran cuentas activas con rol DOCENTE y la contraseña correcta
class SesionDocenteServiceTest {

    private static final String CLAVE = "clave-de-prueba";
    private static final String CIFRADA = "bcrypt$" + new BCryptPasswordEncoder(4).encode(CLAVE);
    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-11-26T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 26, 10, 0);

    private final CuentaRepository cuentas = mock(CuentaRepository.class);
    private final SesionDocenteRepository sesiones = mock(SesionDocenteRepository.class);
    private SesionDocenteService service;

    @BeforeEach
    void preparar() {
        service = new SesionDocenteService(cuentas, sesiones, new FrenoIntentos(RELOJ), RELOJ);
    }

    private void cuenta(boolean activa, String rol) {
        when(cuentas.porUsuario("prueba")).thenReturn(Optional.of(new CuentaDocente(
                15L, "prueba", CIFRADA, activa, "Docente", "De Prueba", rol)));
    }

    private ReglaNegocioException fallaAlEntrar(String usuario, String clave) {
        return assertThrows(ReglaNegocioException.class, () -> service.iniciar(usuario, clave));
    }

    @Test
    void entraConLaCuentaYDevuelveSuIdYNombre() {
        cuenta(true, "DOCENTE");

        SesionDocenteResponse sesion = service.iniciar("  prueba ", CLAVE);

        assertEquals(new SesionDocenteResponse.Docente(15L, "prueba", "Docente De Prueba"),
                sesion.docente());
        assertNotEquals(sesion.access(), sesion.refresh());
    }

    @Test
    void guardaSoloLasHuellasDeLosTokensConSuVigencia() {
        cuenta(true, "DOCENTE");

        SesionDocenteResponse sesion = service.iniciar("prueba", CLAVE);

        ArgumentCaptor<String> acceso = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> refresh = ArgumentCaptor.forClass(String.class);
        verify(sesiones).crear(eq(15L), acceso.capture(), eq(AHORA.plusHours(1)),
                refresh.capture(), eq(AHORA.plusDays(1)));
        assertEquals(SesionDocenteService.huellaDe(sesion.access()), acceso.getValue());
        assertEquals(SesionDocenteService.huellaDe(sesion.refresh()), refresh.getValue());
        assertNotEquals(sesion.access(), acceso.getValue());
    }

    @Test
    void cadaInicioDeSesionEntregaTokensDistintos() {
        cuenta(true, "DOCENTE");

        assertNotEquals(service.iniciar("prueba", CLAVE).access(),
                service.iniciar("prueba", CLAVE).access());
    }

    @Test
    void unaContrasenaIncorrectaYUnUsuarioInexistenteDanElMismoAviso() {
        cuenta(true, "DOCENTE");
        when(cuentas.porUsuario("nadie")).thenReturn(Optional.empty());

        ReglaNegocioException mala = fallaAlEntrar("prueba", "otra");
        ReglaNegocioException inexistente = fallaAlEntrar("nadie", CLAVE);

        assertEquals(HttpStatus.UNAUTHORIZED, mala.getEstado());
        assertEquals(mala.getMessage(), inexistente.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, inexistente.getEstado());
        verifyNoInteractions(sesiones);
    }

    @Test
    void unaCuentaDesactivadaNoEntra() {
        cuenta(false, "DOCENTE");

        assertEquals("Tu cuenta está desactivada. Contacta al administrador",
                fallaAlEntrar("prueba", CLAVE).getMessage());
        verifyNoInteractions(sesiones);
    }

    @Test
    void sinLaContrasenaNoSeRevelaQueLaCuentaEstaDesactivada() {
        cuenta(false, "DOCENTE");

        assertEquals("Usuario o contraseña incorrectos",
                fallaAlEntrar("prueba", "otra").getMessage());
    }

    @Test
    void unAdministradorOUnaCuentaSinRolNoEntran() {
        for (String rol : new String[]{"ADMIN", null}) {
            cuenta(true, rol);

            assertEquals(HttpStatus.FORBIDDEN, fallaAlEntrar("prueba", CLAVE).getEstado());
        }
        verifyNoInteractions(sesiones);
    }

    @Test
    void trasCincoFallosSeBloqueaAunqueLuegoAcierte() {
        cuenta(true, "DOCENTE");
        for (int i = 0; i < FrenoIntentos.MAXIMO_FALLOS; i++) {
            fallaAlEntrar("prueba", "otra");
        }

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, fallaAlEntrar("PRUEBA", CLAVE).getEstado());
        verify(cuentas, times(FrenoIntentos.MAXIMO_FALLOS)).porUsuario(anyString());
    }

    @Test
    void unTokenVigenteDevuelveElDocente() {
        when(sesiones.docentePorAcceso(SesionDocenteService.huellaDe("abc"), AHORA))
                .thenReturn(Optional.of(15L));

        assertEquals(15L, service.docenteDe("abc"));
    }

    @Test
    void unTokenQueNoSirveDa401() {
        when(sesiones.docentePorAcceso(anyString(), any())).thenReturn(Optional.empty());

        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ReglaNegocioException.class,
                () -> service.docenteDe("viejo")).getEstado());
    }

    @Test
    void renuevaElAccesoSiLaSesionSigueVigente() {
        when(sesiones.renovarAcceso(eq(SesionDocenteService.huellaDe("r")), anyString(),
                eq(AHORA.plusHours(1)), eq(AHORA))).thenReturn(true);

        assertNotEquals("r", service.renovar("r"));
    }

    @Test
    void noRenuevaUnaSesionVencidaOCerrada() {
        when(sesiones.renovarAcceso(anyString(), anyString(), any(), any())).thenReturn(false);

        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ReglaNegocioException.class,
                () -> service.renovar("r")).getEstado());
    }

    @Test
    void cerrarSesionBorraLaSesion() {
        service.cerrar("r");

        verify(sesiones).cerrar(SesionDocenteService.huellaDe("r"));
    }
}
