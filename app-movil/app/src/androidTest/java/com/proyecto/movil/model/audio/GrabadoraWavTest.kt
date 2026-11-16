package com.proyecto.movil.model.audio

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/*
 * T-318 (US-11): graba con el micrófono REAL de la tablet y revisa que el WAV salga
 * como lo pide el motor de IA (T-327): PCM 16 bits, mono, 16 kHz.
 * Se corre con la tablet conectada (androidTest).
 */
class GrabadoraWavTest {

    private val instrumentacion = InstrumentationRegistry.getInstrumentation()
    private val contexto = instrumentacion.targetContext
    private lateinit var archivo: File

    @Before
    fun preparar() {
        // Da el permiso del micrófono sin que aparezca la ventana
        instrumentacion.uiAutomation.grantRuntimePermission(contexto.packageName, Manifest.permission.RECORD_AUDIO)
        archivo = File(contexto.cacheDir, "prueba_t318.wav").apply { delete() }
    }

    @After
    fun limpiar() {
        archivo.delete()
    }

    @Test
    fun grabaUnWavMono16kHzDe16Bits() {
        val grabadora = GrabadoraWav()
        grabadora.iniciar(archivo)
        assertTrue(grabadora.grabando)

        Thread.sleep(1_500)
        val grabacion = grabadora.detener()!!

        assertFalse(grabadora.grabando)
        assertTrue("debería durar cerca de 1,5 s, duró ${grabacion.duracionMs} ms", grabacion.duracionMs in 1_000L..2_500L)

        val bytes = archivo.readBytes()
        val cabecera = ByteBuffer.wrap(bytes, 0, 44).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(bytes, 0, 4))
        assertEquals("WAVE", String(bytes, 8, 4))
        assertEquals(1, cabecera.getShort(20).toInt())        // PCM
        assertEquals(1, cabecera.getShort(22).toInt())        // mono
        assertEquals(16_000, cabecera.getInt(24))             // 16 kHz
        assertEquals(16, cabecera.getShort(34).toInt())       // 16 bits
        assertEquals(archivo.length() - 44, cabecera.getInt(40).toLong())   // tamaño del audio
    }

    @Test
    fun detenerSinHaberGrabadoNoFalla() {
        val grabadora = GrabadoraWav()
        assertEquals(null, grabadora.detener())
    }

    @Test
    fun elVolumenSeMueveConElSonidoDelAmbiente() {
        // Con la tablet en un aula siempre hay algo de ruido: el nivel no debería quedarse en 0 todo el tiempo
        val grabadora = GrabadoraWav()
        grabadora.iniciar(archivo)
        var maximo = 0f
        repeat(15) {
            Thread.sleep(100)
            maximo = maxOf(maximo, grabadora.nivelVoz)
        }
        grabadora.detener()

        assertTrue("nivelVoz siempre fue 0: ¿el micrófono está tapado o bloqueado?", maximo > 0f)
        assertTrue(maximo <= 1f)
    }
}