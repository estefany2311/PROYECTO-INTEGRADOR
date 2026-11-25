package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.model.PalabraLeida;
import com.proyecto.usuario.docente.service.FormulaFluidez.AnalisisPausas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// T-398 (US-15): ritmo, pausas y puntaje final con lecturas de ejemplo (grado 3: 60 ppm, 2 s de tolerancia)
class FormulaFluidezTest {

    // lectura de ejemplo: cada palabra dura 0.4 s y entre una y otra hay la pausa indicada
    static List<PalabraLeida> lectura(double... pausas) {
        List<PalabraLeida> palabras = new ArrayList<>();
        double tiempo = 0.5; // un poco de silencio antes de empezar a leer
        for (int i = 0; i <= pausas.length; i++) {
            palabras.add(new PalabraLeida("palabra" + i, tiempo, tiempo + 0.4, 0.95));
            tiempo += 0.4 + (i < pausas.length ? pausas[i] : 0);
        }
        return palabras;
    }

    static double[] pausasIguales(int cantidad, double pausa) {
        double[] pausas = new double[cantidad];
        java.util.Arrays.fill(pausas, pausa);
        return pausas;
    }

    @Test
    void palabrasPorMinutoSeMideDesdeLaPrimeraPalabraHastaLaUltima() {
        // 11 palabras en 10.4 s (el silencio inicial no cuenta)
        double ppm = FormulaFluidez.palabrasPorMinuto(lectura(pausasIguales(10, 0.6)));

        assertEquals(63.46, ppm, 0.01);
    }

    @Test
    void conMenosDeDosPalabrasNoHayRitmo() {
        assertEquals(0, FormulaFluidez.palabrasPorMinuto(List.of()));
        assertEquals(0, FormulaFluidez.palabrasPorMinuto(
                List.of(new PalabraLeida("sol", 1.0, 1.4, 0.9))));
    }

    @Test
    void sinTiempoTranscurridoNoHayRitmo() {
        List<PalabraLeida> mismoInstante = List.of(
                new PalabraLeida("el", 2.0, 2.0, 0.9),
                new PalabraLeida("sol", 2.0, 2.0, 0.9));

        assertEquals(0, FormulaFluidez.palabrasPorMinuto(mismoInstante));
    }

    @Test
    void laLecturaSinPausasLargasNiVacilacionesNoTienePenalizacion() {
        AnalisisPausas pausas = FormulaFluidez.analizarPausas(lectura(pausasIguales(10, 0.6)), 2.0);

        assertEquals(0, pausas.pausasLargas());
        assertEquals(0, pausas.vacilaciones());
    }

    @Test
    void distingueUnaPausaLargaDeUnaVacilacion() {
        AnalisisPausas pausas = FormulaFluidez.analizarPausas(
                lectura(0.6, 3.0, 0.6, 0.6, 1.5, 0.6, 0.6, 2.5, 0.6, 0.6), 2.0);

        assertEquals(2, pausas.pausasLargas());   // 3.0 y 2.5 superan los 2.0 s
        assertEquals(1, pausas.vacilaciones());   // 1.5 está entre 1.0 y 2.0
    }

    @Test
    void unaPausaIgualALaToleranciaEsVacilacionYNoPausaLarga() {
        AnalisisPausas pausas = FormulaFluidez.analizarPausas(lectura(2.0), 2.0);

        assertEquals(0, pausas.pausasLargas());
        assertEquals(1, pausas.vacilaciones());
    }

    @Test
    void unaPausaIgualALaMitadDeLaToleranciaTodavíaNoEsVacilacion() {
        AnalisisPausas pausas = FormulaFluidez.analizarPausas(lectura(1.0), 2.0);

        assertEquals(0, pausas.vacilaciones());
    }

    @Test
    void palabrasSolapadasNoCuentanComoPausa() {
        List<PalabraLeida> solapadas = List.of(
                new PalabraLeida("el", 1.0, 1.5, 0.9),
                new PalabraLeida("sol", 1.3, 1.8, 0.9));

        AnalisisPausas pausas = FormulaFluidez.analizarPausas(solapadas, 0.0);

        assertEquals(0, pausas.pausasLargas());
        assertEquals(0, pausas.vacilaciones());
    }

    @Test
    void elRitmoTieneTopeDe100() {
        assertEquals(100, FormulaFluidez.puntajeRitmo(200, 60));
        assertEquals(100, FormulaFluidez.puntajeRitmo(60, 60));
    }

    @Test
    void elRitmoEsProporcionalCuandoLeeMasLento() {
        assertEquals(50, FormulaFluidez.puntajeRitmo(30, 60));
        assertEquals(0, FormulaFluidez.puntajeRitmo(0, 60));
    }

    @Test
    void laPenalizacionDePausasNoBajaDeCero() {
        assertEquals(100, FormulaFluidez.puntajePausas(0, 0));
        assertEquals(77, FormulaFluidez.puntajePausas(2, 1));
        assertEquals(0, FormulaFluidez.puntajePausas(30, 30));
    }

    @Test
    void elPuntajeFinalPesaMasElRitmoQueLasPausas() {
        assertEquals(100, FormulaFluidez.puntajeFluidez(100, 100));
        assertEquals(0, FormulaFluidez.puntajeFluidez(0, 0));
        assertEquals(60, FormulaFluidez.puntajeFluidez(100, 0));
        assertEquals(40, FormulaFluidez.puntajeFluidez(0, 100));
    }

    @Test
    void lecturaFluidaDeEjemploObtiene100() {
        List<PalabraLeida> palabras = lectura(pausasIguales(10, 0.6));

        assertEquals(100, puntaje(palabras));
    }

    @Test
    void lecturaVacilanteDeEjemploObtiene73() {
        List<PalabraLeida> palabras = lectura(0.6, 3.0, 0.6, 0.6, 1.5, 0.6, 0.6, 2.5, 0.6, 0.6);

        // 42 ppm de 60 esperadas (ritmo 71) y 2 pausas largas + 1 vacilación (pausas 77)
        assertEquals(73, puntaje(palabras));
    }

    @Test
    void lecturaLentaPeroSinPausasLargasObtiene89() {
        List<PalabraLeida> palabras = lectura(pausasIguales(10, 0.9));

        // 49 ppm de 60 esperadas (ritmo 82) y ninguna pausa que penalice
        assertEquals(89, puntaje(palabras));
    }

    // el mismo cálculo que hace FluidezService con la configuración del grado 3
    private static int puntaje(List<PalabraLeida> palabras) {
        double ppm = FormulaFluidez.palabrasPorMinuto(palabras);
        AnalisisPausas pausas = FormulaFluidez.analizarPausas(palabras, 2.0);
        return FormulaFluidez.puntajeFluidez(
                FormulaFluidez.puntajeRitmo(ppm, 60),
                FormulaFluidez.puntajePausas(pausas.pausasLargas(), pausas.vacilaciones()));
    }
}
