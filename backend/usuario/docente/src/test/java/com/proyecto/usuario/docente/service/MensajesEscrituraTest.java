package com.proyecto.usuario.docente.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// T-404 (US-22): el mensaje cambia según el puntaje y la parte más débil
class MensajesEscrituraTest {

    @Test
    void unPuntajeExcelenteSinNadaQueMejorarNoLlevaConsejo() {
        assertEquals("¡Excelente! Escribiste muy bien.",
                MensajesEscritura.para(95, 100, 100, 90, 0, 20, 15));
    }

    @Test
    void laFraseDependeDelPuntaje() {
        assertEquals("¡Muy bien! Tu escritura va por buen camino.",
                MensajesEscritura.para(70, 90, 90, 90, 0, 20, 15));
        assertEquals("Buen intento. Con un poco más de práctica vas a mejorar.",
                MensajesEscritura.para(50, 90, 90, 90, 0, 20, 15));
        assertEquals("Sigue practicando, tú puedes.",
                MensajesEscritura.para(49, 90, 90, 90, 0, 20, 15));
    }

    @Test
    void siLoMasDebilEsLaOrtografiaAconsejaRevisarLasPalabras() {
        assertEquals("¡Muy bien! Tu escritura va por buen camino. Revisa las 3 palabras que quedaron subrayadas.",
                MensajesEscritura.para(78, 40, 100, 100, 3, 20, 15));
        assertEquals("¡Muy bien! Tu escritura va por buen camino. Revisa la palabra que quedó subrayada.",
                MensajesEscritura.para(78, 70, 100, 100, 1, 20, 15));
    }

    @Test
    void siLoMasDebilEsLaLongitudDiceCuantasPalabrasFaltaron() {
        assertEquals("Buen intento. Con un poco más de práctica vas a mejorar. Escribe un poco más: te faltaron 7 palabras.",
                MensajesEscritura.para(55, 100, 53, 100, 0, 8, 15));
        assertEquals("Buen intento. Con un poco más de práctica vas a mejorar. Escribe un poco más: te faltó 1 palabra.",
                MensajesEscritura.para(55, 100, 60, 100, 0, 14, 15));
    }

    @Test
    void siLoMasDebilSonLasOracionesRecuerdaLaMayusculaYElPunto() {
        assertEquals("Sigue practicando, tú puedes. Recuerda empezar cada oración con mayúscula y terminarla con punto.",
                MensajesEscritura.para(41, 50, 53, 0, 2, 8, 15));
    }
}
