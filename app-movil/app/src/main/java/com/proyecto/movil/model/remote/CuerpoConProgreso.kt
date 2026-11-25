package com.proyecto.movil.model.remote

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.File

// T-417: manda el archivo por partes y avisa cuánto lleva (de 0 a 1) para la barra de progreso
class CuerpoConProgreso(
    private val archivo: File,
    private val tipo: MediaType?,
    private val alAvanzar: (Float) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = tipo

    override fun contentLength(): Long = archivo.length()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength().coerceAtLeast(1)
        var enviados = 0L
        alAvanzar(0f)
        archivo.source().use { fuente ->
            val buffer = Buffer()
            while (true) {
                val leidos = fuente.read(buffer, BLOQUE)
                if (leidos == -1L) break
                sink.write(buffer, leidos)
                enviados += leidos
                alAvanzar(enviados.toFloat() / total)
            }
        }
    }

    companion object {
        private const val BLOQUE = 8 * 1024L
    }
}
