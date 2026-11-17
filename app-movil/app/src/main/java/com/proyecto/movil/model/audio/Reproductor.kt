package com.proyecto.movil.model.audio

import android.media.MediaPlayer
import java.io.File

// T-407 (US-12): reproduce la grabación para que el niño se escuche antes de enviarla
interface IReproductor {
    val preparado: Boolean
    val reproduciendo: Boolean
    val posicionMs: Long

    fun preparar(archivo: File, alTerminar: () -> Unit)
    fun reproducir()
    fun pausar()
    fun liberar()
}

class ReproductorAudio : IReproductor {

    private var player: MediaPlayer? = null

    override val preparado: Boolean get() = player != null
    override val reproduciendo: Boolean get() = player?.isPlaying == true
    override val posicionMs: Long get() = player?.currentPosition?.toLong() ?: 0L

    override fun preparar(archivo: File, alTerminar: () -> Unit) {
        liberar()
        player = MediaPlayer().apply {
            setDataSource(archivo.path)
            setOnCompletionListener { alTerminar() }
            prepare()
        }
    }

    override fun reproducir() {
        player?.start()
    }

    override fun pausar() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    // T-410: suelta el reproductor y cierra el archivo
    override fun liberar() {
        player?.release()
        player = null
    }
}
