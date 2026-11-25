package com.proyecto.movil.model.remote

import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// T-417: el cuerpo manda todo el archivo y el progreso sube de 0 a 1
class CuerpoConProgresoTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    @Test
    fun mandaElArchivoCompletoYAvisaHastaElCienPorCiento() {
        val datos = ByteArray(100_000) { (it % 256).toByte() }
        val archivo = carpeta.newFile("lectura.wav").apply { writeBytes(datos) }
        val avances = mutableListOf<Float>()
        val cuerpo = CuerpoConProgreso(archivo, "audio/wav".toMediaType()) { avances += it }
        val destino = Buffer()

        cuerpo.writeTo(destino)

        assertArrayEquals(datos, destino.readByteArray())
        assertEquals(100_000L, cuerpo.contentLength())
        assertEquals(0f, avances.first())
        assertEquals(1f, avances.last())
        assertTrue(avances.zipWithNext().all { (a, b) -> b >= a })   // nunca retrocede
    }
}
