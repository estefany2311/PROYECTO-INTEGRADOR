package com.proyecto.movil.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// T-428 (US-14): la lista de palabras por mejorar sale del texto marcado por el backend
class PalabrasPorMejorarTest {

    private fun resultado(palabras: List<PalabraLeida>?) =
        ResultadoEvaluacion(50L, 1, "listo", "", 80, 4, 5, 2, 8, null, null, null, palabras)

    @Test
    fun soloLasMarcadasSinPuntuacionNiRepetidas() {
        val palabras = listOf(
            PalabraLeida("El", false),
            PalabraLeida("cóndor,", true),
            PalabraLeida("vuela.", false),
            PalabraLeida("¡Cóndor!", true),
            PalabraLeida("Andes.", true)
        )

        assertEquals(listOf("cóndor", "Andes"), resultado(palabras).palabrasPorMejorar())
    }

    @Test
    fun siNoLlegaronPalabrasLaListaEstaVacia() {
        // Gson deja null cuando el backend todavía no manda "palabras"
        assertTrue(resultado(null).palabrasPorMejorar().isEmpty())
    }

    @Test
    fun limpiarQuitaSignosPeroDejaTildesYEnie() {
        assertEquals("niño", limpiarPalabra("¿niño?"))
        assertEquals("", limpiarPalabra("—"))
    }
}
