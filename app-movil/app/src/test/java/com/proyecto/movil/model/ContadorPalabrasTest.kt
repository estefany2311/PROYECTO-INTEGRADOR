package com.proyecto.movil.model

import org.junit.Assert.assertEquals
import org.junit.Test

// T-324 (US-18): cómo se cuentan las palabras del texto del niño
class ContadorPalabrasTest {

    @Test
    fun `texto vacio o solo espacios tiene cero palabras`() {
        assertEquals(0, contarPalabras(""))
        assertEquals(0, contarPalabras("   \n\n  "))
    }

    @Test
    fun `cuenta palabras separadas por espacios`() {
        assertEquals(6, contarPalabras("Mi animal favorito es el perro"))
    }

    @Test
    fun `varios espacios y saltos de linea no suman palabras`() {
        assertEquals(3, contarPalabras("  Mi    perro\n\n  juega  "))
    }

    @Test
    fun `los signos no son palabras`() {
        assertEquals(2, contarPalabras("¡Hola, Toby!"))
        assertEquals(0, contarPalabras("... ¡! ¿?"))
        assertEquals(3, contarPalabras("Come, duerme y..."))
    }

    @Test
    fun `tildes ñ y dieresis son parte de la palabra`() {
        assertEquals(4, contarPalabras("El niño juega pingüino"))
        assertEquals(2, contarPalabras("canción árbol"))
    }

    @Test
    fun `los numeros cuentan como palabras`() {
        assertEquals(4, contarPalabras("Tengo 2 perros grandes"))
    }

    @Test
    fun `palabras unidas por guion cuentan por separado`() {
        assertEquals(2, contarPalabras("perro-gato"))
    }

    @Test
    fun `una palabra a medio escribir ya cuenta`() {
        assertEquals(1, contarPalabras("per"))
    }
}