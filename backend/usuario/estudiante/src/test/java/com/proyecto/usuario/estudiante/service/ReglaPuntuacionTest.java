package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.ReglaPuntuacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// T-425: la regla única de puntos y estrellas del equipo
class ReglaPuntuacionTest {

    private final ReglaPuntuacion lectura = new ReglaPuntuacion(10, 50, 70, 90);   // valores de Daniela

    @Test
    void estrellasSegunLosUmbrales() {
        assertEquals(0, lectura.estrellas(49));
        assertEquals(1, lectura.estrellas(50));
        assertEquals(2, lectura.estrellas(70));
        assertEquals(2, lectura.estrellas(89));
        assertEquals(3, lectura.estrellas(90));
        assertEquals(3, lectura.estrellas(100));
    }

    @Test
    void puntosProporcionalesALaNota() {
        assertEquals(8, lectura.puntos(80));
        assertEquals(7, lectura.puntos(71));    // 7,1 se redondea a 7
        assertEquals(10, lectura.puntos(100));
        assertEquals(0, lectura.puntos(0));
    }

    @Test
    void sinFilaEnLaTablaSeUsan100PuntosY60_80_95() {
        assertEquals(80, ReglaPuntuacion.POR_DEFECTO.puntos(80));
        assertEquals(1, ReglaPuntuacion.POR_DEFECTO.estrellas(60));
        assertEquals(2, ReglaPuntuacion.POR_DEFECTO.estrellas(80));
        assertEquals(3, ReglaPuntuacion.POR_DEFECTO.estrellas(95));
    }
}
