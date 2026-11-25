package com.proyecto.movil.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// T-367 (US-20): cambiar la palabra subrayada por la sugerencia elegida
class ReemplazarPalabraTest {

    private val kasa = ErrorOrtografico("kasa", 8, 12, "ortografia", "casa")

    @Test
    fun cambiaSoloEsaPalabra() {
        assertEquals("Vivo en casa grande", reemplazarPalabra("Vivo en kasa grande", kasa, "casa"))
    }

    @Test
    fun funcionaConSugerenciasMasLargas() {
        val arbol = ErrorOrtografico("arbol", 8, 13, "tilde", "árbol")
        assertEquals("Subí al árbolito", reemplazarPalabra("Subí al arbolito", arbol, "árbol"))
    }

    @Test
    fun siLaPalabraYaNoEstaAhiNoCambiaNada() {
        assertNull(reemplazarPalabra("Vivo en casa grande", kasa, "casa"))   // ya la había corregido
        assertNull(reemplazarPalabra("Vivo", kasa, "casa"))                  // borró el resto del texto
    }
}
