package com.proyecto.movil.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// T-366 (US-20): qué sugerencias muestra el menú de una palabra subrayada
class SugerenciasTest {

    @Test
    fun usaLaListaDelBackend() {
        val error = ErrorOrtografico("kasa", 0, 4, "ortografia", "casa", listOf("casa", "caza", "masa"))

        assertEquals(listOf("casa", "caza", "masa"), error.opciones())
    }

    @Test
    fun siNoLlegaLaListaUsaLaUnicaSugerencia() {
        // Gson deja null si el backend todavía no manda "sugerencias"
        assertEquals(listOf("árbol"), ErrorOrtografico("arbol", 0, 5, "tilde", "árbol").opciones())
    }

    @Test
    fun sinSugerenciasLaListaEstaVacia() {
        assertTrue(ErrorOrtografico("xyz", 0, 3, "ortografia", null, emptyList()).opciones().isEmpty())
    }
}
