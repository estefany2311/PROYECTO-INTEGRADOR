package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.AudioRecibido
import com.proyecto.movil.model.ResultadoEnvio
import com.proyecto.movil.model.audio.AlmacenGrabaciones
import com.proyecto.movil.model.audio.Grabacion
import com.proyecto.movil.model.audio.IGrabadora
import com.proyecto.movil.model.audio.IReproductor
import com.proyecto.movil.model.repository.IAudioRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private class GrabadoraQuieta : IGrabadora {
    override val grabando = false
    override val nivelVoz = 0f
    override val duracionMs = 0L
    override fun iniciar(archivo: File) {}
    override fun detener(): Grabacion? = null
}

private class ReproductorQuieto : IReproductor {
    override val preparado = false
    override val reproduciendo = false
    override val posicionMs = 0L
    override fun preparar(archivo: File, alTerminar: () -> Unit) {}
    override fun reproducir() {}
    override fun pausar() {}
    override fun liberar() {}
}

// Avisa medio progreso y espera a que la prueba diga cómo termina el envío
private class AudioRepositoryFalso : IAudioRepository {
    var respuesta = CompletableDeferred<ResultadoEnvio>()
    var envios = 0
    var ultimo: Triple<Int, Long, File>? = null

    override suspend fun enviar(nivelId: Int, lecturaId: Long, archivo: File, alAvanzar: (Float) -> Unit): ResultadoEnvio {
        envios++
        ultimo = Triple(nivelId, lecturaId, archivo)
        alAvanzar(0.5f)
        return respuesta.await()
    }
}

// T-417 (US-13): subir la grabación con barra de progreso y reintento
@OptIn(ExperimentalCoroutinesApi::class)
class EnvioViewModelTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var almacen: AlmacenGrabaciones
    private lateinit var audios: AudioRepositoryFalso
    private lateinit var viewModel: GrabacionViewModel

    private val recibido = AudioRecibido(50L, 1, 5L, 3_000, "procesando")

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        almacen = AlmacenGrabaciones(File(carpeta.root, "grabaciones"))
        audios = AudioRepositoryFalso()
        viewModel = GrabacionViewModel(GrabadoraQuieta(), almacen, { 7L }, ReproductorQuieto(), audios)
        // Una grabación de 1 segundo ya guardada en la tablet
        val wav = File(carpeta.root, "temporal.wav").apply { writeBytes(ByteArray(44 + 32_000)) }
        almacen.guardar(7L, 1, Grabacion(wav, 1_000))
        viewModel.revisarGuardada(1)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun `sin grabacion no se envia nada`() = runTest(dispatcher) {
        viewModel.grabarOtraVez()

        viewModel.enviar(1, 5L)
        runCurrent()

        assertEquals(0, audios.envios)
        assertTrue(viewModel.uiState.value is GrabacionUiState.Lista)
    }

    @Test
    fun `mientras sube muestra el progreso`() = runTest(dispatcher) {
        viewModel.enviar(1, 5L)
        runCurrent()

        val estado = viewModel.uiState.value as GrabacionUiState.Enviando
        assertEquals(0.5f, estado.progreso)
        assertEquals(1, audios.ultimo!!.first)
        assertEquals(5L, audios.ultimo!!.second)

        audios.respuesta.complete(ResultadoEnvio.Exito(recibido))
        runCurrent()
    }

    @Test
    fun `al terminar queda enviada y se borra de la tablet`() = runTest(dispatcher) {
        viewModel.enviar(1, 5L)
        audios.respuesta.complete(ResultadoEnvio.Exito(recibido))
        runCurrent()

        assertEquals(GrabacionUiState.Enviada(50L), viewModel.uiState.value)
        assertNull(almacen.guardada(7L, 1))
    }

    @Test
    fun `sin internet se puede reintentar con la misma grabacion`() = runTest(dispatcher) {
        viewModel.enviar(1, 5L)
        audios.respuesta.complete(ResultadoEnvio.SinConexion)
        runCurrent()

        val error = viewModel.uiState.value as GrabacionUiState.ErrorEnvio
        assertTrue(error.puedeReintentar)
        assertEquals(GrabacionViewModel.MENSAJE_SIN_CONEXION, error.mensaje)
        assertTrue(almacen.guardada(7L, 1) != null)   // la grabación sigue en la tablet

        audios.respuesta = CompletableDeferred(ResultadoEnvio.Exito(recibido))
        viewModel.enviar(1, 5L)
        runCurrent()

        assertEquals(2, audios.envios)
        assertEquals(GrabacionUiState.Enviada(50L), viewModel.uiState.value)
    }

    @Test
    fun `si el servidor la rechaza muestra su mensaje y no ofrece reintentar`() = runTest(dispatcher) {
        viewModel.enviar(1, 5L)
        audios.respuesta.complete(ResultadoEnvio.Rechazado("La grabación es muy corta. Intenta leer otra vez"))
        runCurrent()

        val error = viewModel.uiState.value as GrabacionUiState.ErrorEnvio
        assertFalse(error.puedeReintentar)
        assertEquals("La grabación es muy corta. Intenta leer otra vez", error.mensaje)
    }

    @Test
    fun `con la sesion vencida avisa y no reintenta`() = runTest(dispatcher) {
        viewModel.enviar(1, 5L)
        audios.respuesta.complete(ResultadoEnvio.SesionVencida)
        runCurrent()

        val error = viewModel.uiState.value as GrabacionUiState.ErrorEnvio
        assertFalse(error.puedeReintentar)
        assertEquals(GrabacionViewModel.MENSAJE_SESION_VENCIDA, error.mensaje)
    }

    @Test
    fun `despues de un error puede volver a grabar`() = runTest(dispatcher) {
        viewModel.enviar(1, 5L)
        audios.respuesta.complete(ResultadoEnvio.Rechazado("No sirve"))
        runCurrent()

        viewModel.grabarOtraVez()

        assertTrue(viewModel.uiState.value is GrabacionUiState.Lista)
    }
}
