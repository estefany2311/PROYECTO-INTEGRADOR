package com.proyecto.movil.model.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// T-317: guardado temporal de las grabaciones en la tablet
class AlmacenGrabacionesTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private var ahora = 1_000_000_000_000L
    private lateinit var almacen: AlmacenGrabaciones

    @Before
    fun preparar() {
        almacen = AlmacenGrabaciones(carpeta.root) { ahora }
    }

    // Simula lo que hace la grabadora: cabecera WAV + segundos de audio (32 000 bytes por segundo)
    private fun grabar(estudianteId: Long, nivelId: Int, segundos: Int): Grabacion {
        val archivo = almacen.archivoEnCurso(estudianteId, nivelId)
        archivo.writeBytes(ByteArray(44 + segundos * 32_000))
        return Grabacion(archivo, segundos * 1000L)
    }

    @Test
    fun `guardar deja la grabacion con su nombre final`() {
        val guardada = almacen.guardar(1, 2, grabar(1, 2, 3))

        assertEquals("nivel_2.wav", guardada.archivo.name)
        assertTrue(guardada.archivo.exists())
        assertFalse(almacen.archivoEnCurso(1, 2).exists())
    }

    @Test
    fun `guardada calcula la duracion desde el archivo`() {
        almacen.guardar(1, 2, grabar(1, 2, 5))

        assertEquals(5_000L, almacen.guardada(1, 2)?.duracionMs)
    }

    @Test
    fun `una grabacion nueva reemplaza a la anterior`() {
        almacen.guardar(1, 2, grabar(1, 2, 5))
        almacen.guardar(1, 2, grabar(1, 2, 2))

        assertEquals(2_000L, almacen.guardada(1, 2)?.duracionMs)
    }

    @Test
    fun `descartar un intento no borra la grabacion anterior`() {
        almacen.guardar(1, 2, grabar(1, 2, 5))
        grabar(1, 2, 1)
        almacen.descartar(1, 2)

        assertEquals(5_000L, almacen.guardada(1, 2)?.duracionMs)
    }

    @Test
    fun `cada estudiante tiene sus propias grabaciones`() {
        almacen.guardar(1, 2, grabar(1, 2, 5))

        assertNull(almacen.guardada(9, 2))
    }

    @Test
    fun `borrar quita la grabacion (lo usara US-13 al enviarla)`() {
        almacen.guardar(1, 2, grabar(1, 2, 5))
        almacen.borrar(1, 2)

        assertNull(almacen.guardada(1, 2))
    }

    @Test
    fun `limpiar borra las grabaciones a medias`() {
        grabar(1, 2, 1)
        almacen.limpiar()

        assertFalse(almacen.archivoEnCurso(1, 2).let { it.exists() && it.length() > 0 })
    }

    @Test
    fun `limpiar borra las de mas de 7 dias y deja las recientes`() {
        val vieja = almacen.guardar(1, 2, grabar(1, 2, 5))
        vieja.archivo.setLastModified(ahora - 8L * 24 * 60 * 60 * 1000)
        val reciente = almacen.guardar(1, 3, grabar(1, 3, 5))
        reciente.archivo.setLastModified(ahora - 1L * 24 * 60 * 60 * 1000)

        almacen.limpiar()

        assertNull(almacen.guardada(1, 2))
        assertEquals(5_000L, almacen.guardada(1, 3)?.duracionMs)
    }
}