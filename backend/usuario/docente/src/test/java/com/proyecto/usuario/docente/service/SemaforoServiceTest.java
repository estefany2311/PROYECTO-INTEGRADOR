package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.model.ColorSemaforo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T-281 (US-27): pruebas de la regla del semáforo.
 * Cubre los límites 80/60, el redondeo y los casos sin evaluaciones.
 */
class SemaforoServiceTest {

    private final SemaforoService service = new SemaforoService();

    @Test
    void promedioNuloEsGris() {
        assertEquals(ColorSemaforo.GRIS, service.calcularColor(null));
    }

    @Test
    void promedioNoFinitoEsGris() {
        assertEquals(ColorSemaforo.GRIS, service.calcularColor(Double.NaN));
        assertEquals(ColorSemaforo.GRIS, service.calcularColor(Double.POSITIVE_INFINITY));
        assertEquals(ColorSemaforo.GRIS, service.calcularColor(Double.NEGATIVE_INFINITY));
    }

    @Test
    void ochentaEsVerdeYSetentaYNuevePuntoCuatroEsAmarillo() {
        assertEquals(ColorSemaforo.VERDE, service.calcularColor(80.0));
        assertEquals(ColorSemaforo.AMARILLO, service.calcularColor(79.4));
    }

    @Test
    void setentaYNuevePuntoCincoRedondeaAOchentaYEsVerde() {
        assertEquals(ColorSemaforo.VERDE, service.calcularColor(79.5));
    }

    @Test
    void sesentaEsAmarilloYCincuentaYNuevePuntoCuatroEsRojo() {
        assertEquals(ColorSemaforo.AMARILLO, service.calcularColor(60.0));
        assertEquals(ColorSemaforo.ROJO, service.calcularColor(59.4));
    }

    @Test
    void cincuentaYNuevePuntoCincoRedondeaASesentaYEsAmarillo() {
        assertEquals(ColorSemaforo.AMARILLO, service.calcularColor(59.5));
    }

    @Test
    void extremosDeLaEscalaSonVerdeYRojo() {
        assertEquals(ColorSemaforo.VERDE, service.calcularColor(100.0));
        assertEquals(ColorSemaforo.ROJO, service.calcularColor(0.0));
    }

    @Test
    void promediosIntermediosRespetanLaRegla() {
        assertEquals(ColorSemaforo.VERDE, service.calcularColor(85.0));
        assertEquals(ColorSemaforo.AMARILLO, service.calcularColor(68.0));
        assertEquals(ColorSemaforo.ROJO, service.calcularColor(50.0));
    }
}
