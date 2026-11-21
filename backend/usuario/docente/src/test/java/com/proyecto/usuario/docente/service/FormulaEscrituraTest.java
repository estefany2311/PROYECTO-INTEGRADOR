package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.service.FormulaEscritura.Oraciones;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// T-404 (US-22): errores, longitud y cumplimiento de la consigna con textos de ejemplo
class FormulaEscrituraTest {

    @Test
    void cuentaLasPalabrasSinContarSignosSueltos() {
        assertEquals(2, FormulaEscritura.contarPalabras("Hola, mundo !"));
        assertEquals(3, FormulaEscritura.contarPalabras("  Mi   mamá\nprepara  "));
        assertEquals(0, FormulaEscritura.contarPalabras("   "));
        assertEquals(0, FormulaEscritura.contarPalabras(null));
    }

    @Test
    void cuentaLasOracionesCompletas() {
        Oraciones oraciones = FormulaEscritura.contarOraciones(
                "Mi animal favorito es el cuy. El cuy come pasto. Vive en mi casa.");

        assertEquals(3, oraciones.total());
        assertEquals(3, oraciones.completas());
    }

    @Test
    void unaOracionSinMayusculaOSinPuntoNoEstaCompleta() {
        Oraciones oraciones = FormulaEscritura.contarOraciones(
                "mi gato es negro. Tiene ojos verdes. Le gusta jugar");

        assertEquals(3, oraciones.total());
        assertEquals(1, oraciones.completas());
    }

    @Test
    void aceptaOracionesConSignosDeApertura() {
        Oraciones oraciones = FormulaEscritura.contarOraciones("¿Cómo estás? ¡Qué bien!");

        assertEquals(2, oraciones.total());
        assertEquals(2, oraciones.completas());
    }

    @Test
    void unTextoVacioNoTieneOraciones() {
        assertEquals(new Oraciones(0, 0), FormulaEscritura.contarOraciones(""));
        assertEquals(new Oraciones(0, 0), FormulaEscritura.contarOraciones(null));
    }

    @Test
    void laOrtografiaBajaConLaProporcionDeErrores() {
        assertEquals(100, FormulaEscritura.puntajeOrtografia(0, 10));
        assertEquals(80, FormulaEscritura.puntajeOrtografia(1, 10));
        assertEquals(50, FormulaEscritura.puntajeOrtografia(2, 8));
        assertEquals(0, FormulaEscritura.puntajeOrtografia(5, 10));
    }

    @Test
    void laOrtografiaNoBajaDeCeroNiPuntuaSinPalabras() {
        assertEquals(0, FormulaEscritura.puntajeOrtografia(20, 10));
        assertEquals(0, FormulaEscritura.puntajeOrtografia(0, 0));
    }

    @Test
    void laLongitudEsProporcionalALasPalabrasMinimasConTope100() {
        assertEquals(100, FormulaEscritura.puntajeLongitud(30, 15));
        assertEquals(100, FormulaEscritura.puntajeLongitud(15, 15));
        assertEquals(53, FormulaEscritura.puntajeLongitud(8, 15));
        assertEquals(0, FormulaEscritura.puntajeLongitud(0, 15));
    }

    @Test
    void sinMinimoLaLongitudNoCastiga() {
        assertEquals(100, FormulaEscritura.puntajeLongitud(3, 0));
    }

    @Test
    void elCumplimientoEsLaProporcionDeOracionesCompletas() {
        assertEquals(100, FormulaEscritura.puntajeConsigna(new Oraciones(3, 3)));
        assertEquals(67, FormulaEscritura.puntajeConsigna(new Oraciones(3, 2)));
        assertEquals(0, FormulaEscritura.puntajeConsigna(new Oraciones(0, 0)));
    }

    @Test
    void elPuntajeFinalPesaMasLaOrtografia() {
        assertEquals(100, FormulaEscritura.puntajeEscritura(100, 100, 100));
        assertEquals(0, FormulaEscritura.puntajeEscritura(0, 0, 0));
        assertEquals(50, FormulaEscritura.puntajeEscritura(100, 0, 0));
        assertEquals(30, FormulaEscritura.puntajeEscritura(0, 100, 0));
        assertEquals(20, FormulaEscritura.puntajeEscritura(0, 0, 100));
    }
}
