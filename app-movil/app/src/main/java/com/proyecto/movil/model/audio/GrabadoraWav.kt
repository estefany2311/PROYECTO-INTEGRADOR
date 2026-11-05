package com.proyecto.movil.model.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/*
 * T-314 (US-11): graba la voz del niño en el formato que acordó el equipo para el motor de IA
 * (decisión de T-327): WAV, PCM de 16 bits, mono, 16 kHz. Pesa unos 32 KB por segundo (≈2 MB por minuto).
 */

// Resultado de una grabación terminada
data class Grabacion(val archivo: File, val duracionMs: Long)

// Interfaz: en las pruebas (T-318) se reemplaza por una grabadora falsa, sin micrófono
interface IGrabadora {
    val grabando: Boolean

    // Volumen de la voz en este momento, de 0 a 1 (para el indicador visual de T-315)
    val nivelVoz: Float

    // Milisegundos grabados hasta ahora
    val duracionMs: Long

    fun iniciar(archivo: File)
    fun detener(): Grabacion?
}

class GrabadoraWav : IGrabadora {

    companion object {
        const val FRECUENCIA_HZ = 16_000
        const val CANALES = 1
        const val BITS = 16
        private const val BYTES_POR_SEGUNDO = FRECUENCIA_HZ * CANALES * BITS / 8   // 32 000
        private const val TAMANO_CABECERA = 44
    }

    @Volatile override var grabando = false
        private set
    @Volatile override var nivelVoz = 0f
        private set
    @Volatile private var bytesGrabados = 0L

    override val duracionMs: Long get() = bytesGrabados * 1000 / BYTES_POR_SEGUNDO

    private var grabadora: AudioRecord? = null
    private var hilo: Thread? = null
    private var archivo: File? = null

    // El permiso RECORD_AUDIO ya lo pidió ConPermisoMicrofono (T-313) antes de llegar aquí
    @SuppressLint("MissingPermission")
    override fun iniciar(archivo: File) {
        if (grabando) return
        archivo.parentFile?.mkdirs()
        val minimo = AudioRecord.getMinBufferSize(FRECUENCIA_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val buffer = maxOf(minimo, BYTES_POR_SEGUNDO / 5)       // al menos 0,2 s de audio
        // VOICE_RECOGNITION: fuente pensada para reconocer voz (menos procesamiento que la de llamadas)
        val audio = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, FRECUENCIA_HZ,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer
        )
        check(audio.state == AudioRecord.STATE_INITIALIZED) { "No se pudo abrir el micrófono" }

        this.archivo = archivo
        this.grabadora = audio
        bytesGrabados = 0
        grabando = true
        audio.startRecording()

        hilo = Thread({ escribir(audio, archivo, buffer) }, "grabadora-wav").apply { start() }
    }

    // Lee el micrófono y va guardando en el archivo; la cabecera WAV se completa al terminar
    private fun escribir(audio: AudioRecord, archivo: File, tamano: Int) {
        val datos = ByteArray(tamano)
        BufferedOutputStream(FileOutputStream(archivo)).use { salida ->
            salida.write(ByteArray(TAMANO_CABECERA))           // lugar para la cabecera
            while (grabando) {
                val leidos = audio.read(datos, 0, datos.size)
                if (leidos > 0) {
                    salida.write(datos, 0, leidos)
                    bytesGrabados += leidos
                    nivelVoz = volumen(datos, leidos)
                }
            }
        }
    }

    override fun detener(): Grabacion? {
        if (!grabando) return null
        grabando = false
        hilo?.join(1_000)
        grabadora?.run {
            stop()
            release()
        }
        grabadora = null
        hilo = null
        nivelVoz = 0f
        val destino = archivo ?: return null
        escribirCabecera(destino, bytesGrabados)
        return Grabacion(destino, duracionMs)
    }

    // Volumen aproximado (0..1): el pico de las muestras de este pedazo
    private fun volumen(datos: ByteArray, leidos: Int): Float {
        var pico = 0
        var i = 0
        while (i + 1 < leidos) {
            val muestra = (datos[i].toInt() and 0xFF) or (datos[i + 1].toInt() shl 8)
            pico = maxOf(pico, abs(muestra.toShort().toInt()))
            i += 2
        }
        return pico / 32_768f
    }

    // Cabecera RIFF/WAVE estándar de 44 bytes, en little-endian
    private fun escribirCabecera(archivo: File, bytesDeAudio: Long) {
        val cabecera = ByteBuffer.allocate(TAMANO_CABECERA).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt((36 + bytesDeAudio).toInt()); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1)                    // PCM
            putShort(CANALES.toShort()); putInt(FRECUENCIA_HZ); putInt(BYTES_POR_SEGUNDO)
            putShort((CANALES * BITS / 8).toShort()); putShort(BITS.toShort())
            put("data".toByteArray()); putInt(bytesDeAudio.toInt())
        }.array()
        RandomAccessFile(archivo, "rw").use { it.seek(0); it.write(cabecera) }
    }
}
