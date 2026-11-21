package com.proyecto.movil.model.audio

import androidx.work.ListenableWorker.Result
import com.proyecto.movil.model.AudioRecibido
import com.proyecto.movil.model.ResultadoEnvio
import com.proyecto.movil.model.repository.IAudioRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private class AudiosConRespuesta(private val respuesta: ResultadoEnvio) : IAudioRepository {
    var envios = 0
    override suspend fun enviar(nivelId: Int, lecturaId: Long, archivo: File, alAvanzar: (Float) -> Unit): ResultadoEnvio {
        envios++
        return respuesta
    }
}

// T-419 (US-13): el envío pendiente que hace WorkManager cuando vuelve el internet
class EnvioPendienteWorkerTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private lateinit var almacen: AlmacenGrabaciones
    private val exito = ResultadoEnvio.Exito(AudioRecibido(50L, 1, 5L, 1_000, "procesando"))

    @Before
    fun preparar() {
        almacen = AlmacenGrabaciones(File(carpeta.root, "grabaciones"))
        val wav = File(carpeta.root, "temporal.wav").apply { writeBytes(ByteArray(44 + 32_000)) }
        almacen.guardar(7L, 1, Grabacion(wav, 1_000))
    }

    private suspend fun enviar(audios: IAudioRepository, sesion: Long? = 7L, intento: Int = 0) =
        EnvioPendienteWorker.enviarPendiente(7L, 1, 5L, intento, almacen, { sesion }, audios)

    @Test
    fun alVolverElInternetLaEnviaYLaBorraDeLaTablet() = runTest {
        val audios = AudiosConRespuesta(exito)

        assertEquals(Result.success(), enviar(audios))
        assertEquals(1, audios.envios)
        assertNull(almacen.guardada(7L, 1))
    }

    @Test
    fun siTodaviaNoHayConexionReintentaDespues() = runTest {
        assertEquals(Result.retry(), enviar(AudiosConRespuesta(ResultadoEnvio.SinConexion)))
        assertNotNull(almacen.guardada(7L, 1))
    }

    @Test
    fun despuesDeMuchosIntentosSeRinde() = runTest {
        val resultado = enviar(AudiosConRespuesta(ResultadoEnvio.SinConexion), intento = EnvioPendienteWorker.INTENTOS_MAXIMOS - 1)

        assertEquals(Result.failure(), resultado)
        assertNotNull(almacen.guardada(7L, 1))   // sigue en la tablet para enviarla a mano
    }

    @Test
    fun siEntroOtroNinoEsperaYNoLaManda() = runTest {
        val audios = AudiosConRespuesta(exito)

        assertEquals(Result.retry(), enviar(audios, sesion = 8L))
        assertEquals(0, audios.envios)
    }

    @Test
    fun siElServidorLaRechazaNoReintenta() = runTest {
        assertEquals(Result.failure(), enviar(AudiosConRespuesta(ResultadoEnvio.Rechazado("muy corta"))))
        assertNotNull(almacen.guardada(7L, 1))
    }

    @Test
    fun siYaNoHayGrabacionTerminaSinHacerNada() = runTest {
        almacen.borrar(7L, 1)
        val audios = AudiosConRespuesta(exito)

        assertEquals(Result.success(), enviar(audios))
        assertEquals(0, audios.envios)
    }
}
