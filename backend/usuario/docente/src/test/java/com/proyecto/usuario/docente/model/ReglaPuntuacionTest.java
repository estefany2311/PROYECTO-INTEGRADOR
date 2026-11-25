package com.proyecto.usuario.docente.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T-305 (US-09): umbrales de estrellas y validaciones de la regla de puntuación
 * que aplica la US-53.
 */
class ReglaPuntuacionTest {

    private final ReglaPuntuacion regla =
            new ReglaPuntuacion("lectura", 100, 60, 80, 95);

    @Test
    void asignaLasEstrellasSegunLosUmbrales() {
        assertEquals(0, regla.estrellasPara(0));
        assertEquals(0, regla.estrellasPara(59));
        assertEquals(1, regla.estrellasPara(60));
        assertEquals(1, regla.estrellasPara(79));
        assertEquals(2, regla.estrellasPara(80));
        assertEquals(2, regla.estrellasPara(94));
        assertEquals(3, regla.estrellasPara(95));
        assertEquals(3, regla.estrellasPara(100));
    }

    @Test
    void rechazaUmbralesQueNoSonCrecientes() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReglaPuntuacion("lectura", 100, 80, 60, 95));
        assertThrows(IllegalArgumentException.class,
                () -> new ReglaPuntuacion("lectura", 100, 60, 60, 95));
    }

    @Test
    void rechazaPuntosNegativos() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReglaPuntuacion("lectura", -1, 60, 80, 95));
    }
}
