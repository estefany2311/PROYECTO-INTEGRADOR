package com.proyecto.movil.model.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// T-438 (US-21): la tablet recuerda qué texto se envió (T-436) y cuál espera internet (T-437)
class TextoEnviadoAlmacenTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val almacen by lazy { AlmacenBorradores(carpeta.newFolder("borradores")) }

    @Test
    fun alMarcarEnviadoGuardaElTextoYBorraElBorrador() {
        almacen.guardar(7L, 2, "Mi perro se llama Toby.")

        almacen.marcarEnviado(7L, 2, "Mi perro se llama Toby.")

        assertEquals("Mi perro se llama Toby.", almacen.enviado(7L, 2))
        assertNull(almacen.leer(7L, 2))
    }

    @Test
    fun elEnvioEsDeCadaAlumnoYNivel() {
        almacen.marcarEnviado(7L, 2, "Texto de Ana")

        assertNull(almacen.enviado(8L, 2))   // otro alumno en la misma tablet
        assertNull(almacen.enviado(7L, 5))   // otro nivel del mismo alumno
    }

    @Test
    fun laMarcaDePendienteSePoneYSeQuita() {
        assertFalse(almacen.pendiente(7L, 2))

        almacen.marcarPendiente(7L, 2, true)
        assertTrue(almacen.pendiente(7L, 2))

        almacen.marcarPendiente(7L, 2, false)
        assertFalse(almacen.pendiente(7L, 2))
    }

    @Test
    fun alEnviarseYaNoQuedaPendiente() {
        almacen.marcarPendiente(7L, 2, true)

        almacen.marcarEnviado(7L, 2, "Mi perro se llama Toby.")

        assertFalse(almacen.pendiente(7L, 2))
    }
}
