package com.proyecto.movil.model.local

import java.io.File

class AlmacenBorradores(
    private val raiz: File,
    private val ahora: () -> Long = System::currentTimeMillis
) {

    companion object {
        const val DIAS_MAXIMOS = 30
        private const val DIA_MS = 24L * 60 * 60 * 1000
    }

    private fun archivo(estudianteId: Long, nivelId: Int) = File(File(raiz, "est_$estudianteId"), "nivel_$nivelId.txt")

    fun leer(estudianteId: Long, nivelId: Int): String? {
        val archivo = archivo(estudianteId, nivelId)
        return if (archivo.isFile) archivo.readText(Charsets.UTF_8).takeIf { it.isNotEmpty() } else null
    }

    fun guardar(estudianteId: Long, nivelId: Int, texto: String) {
        val destino = archivo(estudianteId, nivelId)
        if (texto.isBlank()) {
            destino.delete()
            return
        }
        destino.parentFile?.mkdirs()
        val temporal = File(destino.parentFile, destino.name + ".tmp")
        temporal.writeText(texto, Charsets.UTF_8)
        if (!temporal.renameTo(destino)) {
            temporal.copyTo(destino, overwrite = true)
            temporal.delete()
        }
    }

    // Lo usará el envío del texto: una vez enviado ya no hace falta el borrador
    fun borrar(estudianteId: Long, nivelId: Int) {
        archivo(estudianteId, nivelId).delete()
    }

    fun limpiar() {
        val limite = ahora() - DIAS_MAXIMOS * DIA_MS
        raiz.listFiles()?.filter { it.isDirectory }?.forEach { carpeta ->
            carpeta.listFiles()?.forEach { archivo ->
                if (archivo.name.endsWith(".tmp") || archivo.lastModified() < limite) archivo.delete()
            }
            if (carpeta.listFiles().isNullOrEmpty()) carpeta.delete()
        }
    }
}
