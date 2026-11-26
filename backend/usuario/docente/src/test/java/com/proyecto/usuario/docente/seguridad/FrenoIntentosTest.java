package com.proyecto.usuario.docente.seguridad;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// login docente: freno contra la adivinación de contraseñas
class FrenoIntentosTest {

    private static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-11-26T10:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final RelojManual reloj = new RelojManual();
    private final FrenoIntentos freno = new FrenoIntentos(reloj);

    private void fallar(int veces) {
        for (int i = 0; i < veces; i++) {
            freno.registrarFallo("prueba");
        }
    }

    @Test
    void conCuatroFallosTodaviaDejaProbar() {
        fallar(4);

        assertFalse(freno.estaBloqueado("prueba"));
    }

    @Test
    void alQuintoFalloBloqueaSoloAEseUsuario() {
        fallar(5);

        assertTrue(freno.estaBloqueado("Prueba"));
        assertFalse(freno.estaBloqueado("otra"));
    }

    @Test
    void elBloqueoTerminaSolo() {
        fallar(5);
        reloj.ahora = reloj.ahora.plus(FrenoIntentos.BLOQUEO);

        assertFalse(freno.estaBloqueado("prueba"));
    }

    @Test
    void unAciertoReiniciaLaCuenta() {
        fallar(4);
        freno.limpiar("prueba");
        fallar(4);

        assertFalse(freno.estaBloqueado("prueba"));
    }
}
