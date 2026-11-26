package com.proyecto.movil.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MensajeDocenteTest {

    private fun mensaje(tipo: String?, fecha: String?) = MensajeDocente(1L, 1, "Mi primera lectura", tipo, "Muy bien", fecha)

    @Test
    fun laFechaSeMuestraConDiaYMes() {
        assertEquals("26 nov", fechaCorta("2026-11-26T04:30:00"))
        assertEquals("3 set", fechaCorta("2026-09-03T10:00:00"))
    }

    @Test
    fun unaFechaRaraOVaciaNoSeMuestra() {
        assertEquals("", fechaCorta(null))
        assertEquals("", fechaCorta("ayer"))
        assertEquals("", fechaCorta("2026-13-01"))
    }

    @Test
    fun elTipoDiceSiFueLecturaOEscritura() {
        assertEquals("Lectura", mensaje("audio", null).nombreReto())
        assertEquals("Escritura", mensaje("texto", null).nombreReto())
        assertEquals("Reto", mensaje(null, null).nombreReto())
    }
}
