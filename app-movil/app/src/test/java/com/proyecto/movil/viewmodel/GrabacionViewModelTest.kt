package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.audio.AlmacenGrabaciones
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// Grabadora sin micrófono: la prueba decide cuánto tiempo "lleva grabado"
private class GrabadoraFalsa : IGrabadora {
    override var grabando = false
    override var nivelVoz = 0.5f
    override var duracionMs = 0L
    var vecesDetenida = 0
    var archivo: File? = null

    override fun iniciar(archivo: File) {
        this.archivo = archivo
        archivo.writeBytes(ByteArray(44 + 32_000))   // cabecera + 1 segundo de "audio"
        grabando = true
    }

    override fun detener(): Grabacion? {
        if (!grabando) return null
        grabando = false
        vecesDetenida++
        return Grabacion(archivo!!, duracionMs)
    }
}

// T-316: la grabación se detiene sola a los 2 minutos · T-317: queda guardada en la tablet
@OptIn(ExperimentalCoroutinesApi::class)
class GrabacionViewModelTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var grabadora: GrabadoraFalsa
    private lateinit var almacen: AlmacenGrabaciones
    private lateinit var viewModel: GrabacionViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        grabadora = GrabadoraFalsa()
        almacen = AlmacenGrabaciones(carpeta.root)
        viewModel = GrabacionViewModel(grabadora, almacen, estudianteId = { 7L })
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

    @Test
    fun `al terminar la grabacion queda guardada para ese estudiante y nivel`() = runTest(dispatcher) {
        viewModel.grabar(3)
        grabadora.duracionMs = 20_000
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)
        viewModel.terminar()

        val estado = viewModel.uiState.value as GrabacionUiState.Grabada
        assertEquals("nivel_3.wav", estado.grabacion.archivo.name)
        assertTrue(estado.grabacion.archivo.exists())
        assertTrue(almacen.guardada(7L, 3) != null)
    }

    @Test
    fun `al volver a la pantalla se muestra la grabacion guardada`() = runTest(dispatcher) {
        viewModel.grabar(3)
        grabadora.duracionMs = 20_000
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)
        viewModel.terminar()

        // Otra pantalla nueva (como si cerrara y abriera la app)
        val otra = GrabacionViewModel(GrabadoraFalsa(), almacen, estudianteId = { 7L })
        otra.revisarGuardada(3)

        val estado = otra.uiState.value as GrabacionUiState.Grabada
        assertTrue(estado.deAntes)
    }

    @Test
    fun `otro estudiante no ve la grabacion`() = runTest(dispatcher) {
        viewModel.grabar(3)
        grabadora.duracionMs = 20_000
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)
        viewModel.terminar()

        val deOtroNino = GrabacionViewModel(GrabadoraFalsa(), almacen, estudianteId = { 8L })
        deOtroNino.revisarGuardada(3)

        assertTrue(deOtroNino.uiState.value is GrabacionUiState.Lista)
    }
}