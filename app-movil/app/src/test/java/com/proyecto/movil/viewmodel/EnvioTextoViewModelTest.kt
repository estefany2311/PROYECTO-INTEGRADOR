package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.ResultadoEnvioTexto
import com.proyecto.movil.model.ResultadoEscritura
import com.proyecto.movil.model.ResultadoOrtografia
import com.proyecto.movil.model.ResultadoReto
import com.proyecto.movil.model.TextoRecibido
import com.proyecto.movil.model.local.AlmacenBorradores
import com.proyecto.movil.model.remote.IMonitorRed
import com.proyecto.movil.model.repository.IOrtografiaRepository
import com.proyecto.movil.model.repository.ITextoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// T-435 (US-21): confirmar y enviar el texto final del editor
@OptIn(ExperimentalCoroutinesApi::class)
class EnvioTextoViewModelTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var almacen: AlmacenBorradores
    private val textos = TextosFalsos()

    private class TextosFalsos : ITextoRepository {
        var respuesta: ResultadoEnvioTexto = ResultadoEnvioTexto.SinConexion
        val enviados = mutableListOf<Pair<Int, String>>()
        override suspend fun enviar(nivelId: Int, texto: String): ResultadoEnvioTexto {
            enviados += nivelId to texto
            return respuesta
        }
    }

    private val ortografia = object : IOrtografiaRepository {
        override suspend fun verificar(texto: String) = ResultadoOrtografia.Exito(emptyList())
    }

    private val red = object : IMonitorRed {
        override val conectado = MutableStateFlow(true)
    }

    private lateinit var viewModel: EditorViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        almacen = AlmacenBorradores(carpeta.newFolder("borradores"))
        viewModel = EditorViewModel(almacen, { 7L }, dispatcher, ortografia, red, textos)
        viewModel.abrir(2)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun pideConfirmarAntesDeEnviarYSePuedeCancelar() = runTest(dispatcher) {
        viewModel.escribir("Mi perro se llama Toby.")
        viewModel.pedirEnviar()
        assertEquals(EnvioTextoUiState.Confirmando, viewModel.envio.value)

        viewModel.cancelarEnvio()

        assertEquals(EnvioTextoUiState.Escribiendo, viewModel.envio.value)
        assertTrue(textos.enviados.isEmpty())
    }

    @Test
    fun alConfirmarEnviaElTextoYBorraElBorrador() = runTest(dispatcher) {
        val escritura = ResultadoEscritura(85, 2, "¡Muy bien!")
        val reto = ResultadoReto(2, "completado", 17, 2, true, 1, 3, "Lee y cuenta", 17, true)
        textos.respuesta = ResultadoEnvioTexto.Exito(TextoRecibido(80L, 2, 5, emptyList()), escritura, reto)
        viewModel.escribir("Mi perro se llama Toby.")
        advanceUntilIdle()                                  // el borrador ya quedó en la tablet
        assertNotNull(almacen.leer(7L, 2))

        viewModel.pedirEnviar()
        viewModel.confirmarEnvio()
        advanceUntilIdle()

        assertEquals(listOf(2 to "Mi perro se llama Toby."), textos.enviados)
        assertEquals(EnvioTextoUiState.Enviado(escritura, reto), viewModel.envio.value)
        assertNull(almacen.leer(7L, 2))
    }

    // T-437
    @Test
    fun siElServidorNoRespondeQuedaEnColaYElBorradorSigueEnLaTablet() = runTest(dispatcher) {
        textos.respuesta = ResultadoEnvioTexto.SinConexion
        viewModel.escribir("Mi perro se llama Toby.")
        advanceUntilIdle()

        viewModel.pedirEnviar()
        viewModel.confirmarEnvio()
        advanceUntilIdle()

        assertEquals(EnvioTextoUiState.EnCola, viewModel.envio.value)
        assertEquals("Mi perro se llama Toby.", almacen.leer(7L, 2))
        assertTrue(almacen.pendiente(7L, 2))
    }

    @Test
    fun sinRedNiSeIntentaYAlVolverElInternetSeEnviaSolo() = runTest(dispatcher) {
        red.conectado.value = false
        viewModel.escribir("Mi perro se llama Toby.")
        viewModel.pedirEnviar()
        viewModel.confirmarEnvio()
        advanceUntilIdle()
        assertEquals(EnvioTextoUiState.EnCola, viewModel.envio.value)
        assertTrue(textos.enviados.isEmpty())

        textos.respuesta = ResultadoEnvioTexto.Exito(TextoRecibido(80L, 2, 5, emptyList()), null, null)
        red.conectado.value = true
        advanceUntilIdle()

        assertEquals(1, textos.enviados.size)
        assertEquals(EnvioTextoUiState.Enviado(null, null), viewModel.envio.value)
    }

    @Test
    fun enColaPuedeIntentarAhora() = runTest(dispatcher) {
        textos.respuesta = ResultadoEnvioTexto.SinConexion
        viewModel.escribir("Mi perro se llama Toby.")
        viewModel.pedirEnviar()
        viewModel.confirmarEnvio()
        advanceUntilIdle()

        textos.respuesta = ResultadoEnvioTexto.Exito(TextoRecibido(80L, 2, 5, emptyList()), null, null)
        viewModel.reintentarEnvio()
        advanceUntilIdle()

        assertEquals(2, textos.enviados.size)
        assertEquals(EnvioTextoUiState.Enviado(null, null), viewModel.envio.value)
    }

    @Test
    fun siElBackendLoRechazaMuestraSuMensajeYPuedeIntentarOtraVez() = runTest(dispatcher) {
        textos.respuesta = ResultadoEnvioTexto.Rechazado("Te faltan 4 palabras para enviar tu texto")
        viewModel.escribir("Mi perro.")

        viewModel.pedirEnviar()
        viewModel.confirmarEnvio()
        advanceUntilIdle()
        assertEquals(EnvioTextoUiState.Error("Te faltan 4 palabras para enviar tu texto"), viewModel.envio.value)

        viewModel.pedirEnviar()
        assertEquals(EnvioTextoUiState.Confirmando, viewModel.envio.value)
    }

    @Test
    fun sinTextoNoPideConfirmar() {
        viewModel.pedirEnviar()

        assertEquals(EnvioTextoUiState.Escribiendo, viewModel.envio.value)
    }
}
