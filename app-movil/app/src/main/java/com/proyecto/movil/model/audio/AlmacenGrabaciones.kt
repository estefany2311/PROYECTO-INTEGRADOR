package com.proyecto.movil.model.audio

import java.io.File


class AlmacenGrabaciones(
    private val raiz: File,
    private val ahora: () -> Long = System::currentTimeMillis
) {

    companion object {
        const val DIAS_MAXIMOS = 7
        private const val DIA_MS = 24L * 60 * 60 * 1000
        private const val SUFIJO_EN_CURSO = "_grabando.wav"
        private const val TAMANO_CABECERA = 44
        private const val BYTES_POR_SEGUNDO = GrabadoraWav.FRECUENCIA_HZ * GrabadoraWav.CANALES * GrabadoraWav.BITS / 8
    }

    private fun carpeta(estudianteId: Long) = File(raiz, "est_$estudianteId")

    private fun archivoFinal(estudianteId: Long, nivelId: Int) = File(carpeta(estudianteId), "nivel_$nivelId.wav")

    // Donde escribe la grabadora mientras el niño lee
    fun archivoEnCurso(estudianteId: Long, nivelId: Int): File =
        File(carpeta(estudianteId), "nivel_$nivelId$SUFIJO_EN_CURSO").also { it.parentFile?.mkdirs() }

    // La grabación salió bien: reemplaza a la que hubiera de ese nivel
    fun guardar(estudianteId: Long, nivelId: Int, grabacion: Grabacion): Grabacion {
        val destino = archivoFinal(estudianteId, nivelId)
        if (destino.exists()) destino.delete()
        if (!grabacion.archivo.renameTo(destino)) {
            grabacion.archivo.copyTo(destino, overwrite = true)
            grabacion.archivo.delete()
        }
        return Grabacion(destino, grabacion.duracionMs)
    }

    // Salió mal o el niño se fue a la mitad: se borra lo que se estaba grabando
    fun descartar(estudianteId: Long, nivelId: Int) {
        archivoEnCurso(estudianteId, nivelId).delete()
    }

    // La grabación que quedó guardada de ese nivel (aunque se haya cerrado la app)
    fun guardada(estudianteId: Long, nivelId: Int): Grabacion? {
        val archivo = archivoFinal(estudianteId, nivelId)
        if (!archivo.isFile || archivo.length() <= TAMANO_CABECERA) return null
        val duracionMs = (archivo.length() - TAMANO_CABECERA) * 1000 / BYTES_POR_SEGUNDO
        return Grabacion(archivo, duracionMs)
    }

    // US-13 la llamará después de enviar la grabación
    fun borrar(estudianteId: Long, nivelId: Int) {
        archivoFinal(estudianteId, nivelId).delete()
    }

    // Al abrir la app: borra grabaciones a medias y las de más de 7 días
    fun limpiar() {
        val limite = ahora() - DIAS_MAXIMOS * DIA_MS
        raiz.listFiles()?.filter { it.isDirectory }?.forEach { carpeta ->
            carpeta.listFiles()?.forEach { archivo ->
                if (archivo.name.endsWith(SUFIJO_EN_CURSO) || archivo.lastModified() < limite) archivo.delete()
            }
            if (carpeta.listFiles().isNullOrEmpty()) carpeta.delete()
        }
    }
}
