package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.audio.Grabacion
import com.proyecto.movil.model.audio.IGrabadora
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

// Grabadora sin micrófono: la prueba decide cuánto tiempo "lleva grabado"
private class GrabadoraFalsa : IGrabadora {
    override var grabando = false
    override var nivelVoz = 0.5f
    override var duracionMs = 0L
    var vecesDetenida = 0

    override fun iniciar(archivo: File) { grabando = true }

    override fun detener(): Grabacion? {
        if (!grabando) return null
        grabando = false
        vecesDetenida++
        return Grabacion(File("nivel_1.wav"), duracionMs)
    }
}

// T-316: la grabación se detiene sola a los 2 minutos
@OptIn(ExperimentalCoroutinesApi::class)
class GrabacionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var grabadora: GrabadoraFalsa
    private lateinit var viewModel: GrabacionViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        grabadora = GrabadoraFalsa()
        viewModel = GrabacionViewModel(grabadora, File("grabaciones"))
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun `antes de los 2 minutos sigue grabando`() = runTest(dispatcher) {
        viewModel.grabar(1)
        grabadora.duracionMs = 119_900
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)

        val estado = viewModel.uiState.value
        assertTrue(estado is GrabacionUiState.Grabando)
        assertEquals(119_900, (estado as GrabacionUiState.Grabando).duracionMs)
        assertTrue(grabadora.grabando)

        viewModel.terminar()   // apaga el reloj; si no, la prueba nunca termina
    }

    @Test
    fun `al llegar a los 2 minutos se detiene sola`() = runTest(dispatcher) {
        viewModel.grabar(1)
        runCurrent()
        grabadora.duracionMs = GrabacionViewModel.DURACION_MAXIMA_MS
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)

        val estado = viewModel.uiState.value
        assertTrue(estado is GrabacionUiState.Grabada)
        assertTrue((estado as GrabacionUiState.Grabada).porTiempo)
        assertFalse(grabadora.grabando)
        assertEquals(1, grabadora.vecesDetenida)
    }

    @Test
    fun `despues de detenerse sola el reloj ya no cambia el estado`() = runTest(dispatcher) {
        viewModel.grabar(1)
        grabadora.duracionMs = GrabacionViewModel.DURACION_MAXIMA_MS
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)
        advanceTimeBy(5_000)

        assertTrue(viewModel.uiState.value is GrabacionUiState.Grabada)
        assertEquals(1, grabadora.vecesDetenida)
    }

    @Test
    fun `si el nino toca Terminar no es por tiempo`() = runTest(dispatcher) {
        viewModel.grabar(1)
        grabadora.duracionMs = 30_000
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)
        viewModel.terminar()

        val estado = viewModel.uiState.value as GrabacionUiState.Grabada
        assertFalse(estado.porTiempo)
        assertEquals(30_000, estado.grabacion.duracionMs)
    }

    @Test
    fun `grabar otra vez despues del limite vuelve a empezar`() = runTest(dispatcher) {
        viewModel.grabar(1)
        grabadora.duracionMs = GrabacionViewModel.DURACION_MAXIMA_MS
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)

        viewModel.grabarOtraVez()
        assertTrue(viewModel.uiState.value is GrabacionUiState.Lista)

        grabadora.duracionMs = 0
        viewModel.grabar(1)
        runCurrent()
        assertTrue(viewModel.uiState.value is GrabacionUiState.Grabando)

        viewModel.terminar()   // apaga el reloj; si no, la prueba nunca termina
    }
}