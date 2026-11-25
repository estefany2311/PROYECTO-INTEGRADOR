package com.proyecto.movil.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// T-433 (US-16): el mensaje que compara la lectura con su mejor intento anterior en el nivel
class MejorIntentoTest {

    private fun resultado(precision: Int?, anterior: MejorIntento?) =
        ResultadoEvaluacion(50L, 1, "listo", "", precision, 8, 10, 2, 8, null, null, null, emptyList(), anterior)

    @Test
    fun siLeyoMejorQueAntesLoFelicita() {
        assertEquals("¡Superaste tu mejor intento!", resultado(90, MejorIntento(2, 75, 2)).mensajeMejorIntento())
    }

    @Test
    fun siLeyoIgualLoDice() {
        assertEquals("¡Igualaste tu mejor intento!", resultado(75, MejorIntento(1, 75, 2)).mensajeMejorIntento())
    }

    @Test
    fun siLeyoPeorLeRecuerdaSuMejorIntento() {
        assertEquals("Tu mejor intento fue de 2 estrellas. ¡Tú puedes!", resultado(60, MejorIntento(3, 85, 2)).mensajeMejorIntento())
        assertEquals("Tu mejor intento fue de 1 estrella. ¡Tú puedes!", resultado(40, MejorIntento(1, 65, 1)).mensajeMejorIntento())
    }

    @Test
    fun enSuPrimerIntentoNoHayMensaje() {
        assertNull(resultado(80, MejorIntento(0, null, null)).mensajeMejorIntento())
        assertNull(resultado(80, null).mensajeMejorIntento())   // backend anterior: no manda mejorAnterior
    }
}
