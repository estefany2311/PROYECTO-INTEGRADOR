package com.proyecto.usuario.estudiante.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// T-421 (US-14): el texto y lo que escuchó Whisper quedan en la misma forma
class NormalizadorTextoTest {

    private final NormalizadorTexto normalizador = new NormalizadorTexto();

    @Test
    void quitaMayusculasTildesYPuntuacion() {
        assertEquals(List.of("habia", "una", "vez", "un", "condor"),
                normalizador.palabras("¡Había una vez un Cóndor!"));
    }

    @Test
    void laEnieSeQuedaYLaDieresisSeQuita() {
        assertEquals(List.of("el", "niño", "y", "el", "pinguino"), normalizador.palabras("El NIÑO y el pingüino."));
    }

    @Test
    void losNumerosSeEscribenConLetras() {
        assertEquals(List.of("tengo", "tres", "gatos"), normalizador.palabras("Tengo 3 gatos"));
        assertEquals(List.of("treinta", "y", "cinco"), normalizador.palabras("35"));
        assertEquals(List.of("cien"), normalizador.palabras("100"));
        assertEquals(List.of("dos", "mil", "veintiseis"), normalizador.palabras("2026"));
    }

    @Test
    void guionesComillasYSaltosDeLineaSeparanPalabras() {
        assertEquals(List.of("dijo", "ven", "aqui", "luego", "se", "fue"),
                normalizador.palabras("—Dijo: «ven aquí»\nluego-se fue..."));
    }

    @Test
    void textoVacioONuloNoTienePalabras() {
        assertEquals(List.of(), normalizador.palabras(""));
        assertEquals(List.of(), normalizador.palabras(null));
        assertEquals(List.of(), normalizador.palabras("¡¿...?!"));
    }
}
