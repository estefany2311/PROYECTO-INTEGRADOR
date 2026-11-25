package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.proyecto.movil.model.audio.AlmacenGrabaciones
import com.proyecto.movil.model.audio.Grabacion
import com.proyecto.movil.model.audio.IGrabadora
import com.proyecto.movil.model.audio.IReproductor
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private class GrabadoraDePrueba : IGrabadora {
    override var grabando = false
    override var nivelVoz = 0.5f
    override var duracionMs = 0L
    private var archivo: File? = null

    override fun iniciar(archivo: File) {
        this.archivo = archivo
        archivo.writeBytes(ByteArray(44 + 32_000))
        grabando = true
    }

    override fun detener(): Grabacion? {
        if (!grabando) return null
        grabando = false
        return Grabacion(archivo!!, duracionMs)
    }
}

// Reproductor sin parlante: la prueba decide en qué segundo va
private class ReproductorFalso : IReproductor {
    override var preparado = false
    override var reproduciendo = false
    override var posicionMs = 0L
    var archivo: File? = null
    var vecesLiberado = 0
    var fallar = false
    private var alTerminar: () -> Unit = {}

    override fun preparar(archivo: File, alTerminar: () -> Unit) {
        if (fallar) throw IllegalStateException("archivo dañado")
        this.archivo = archivo
        this.alTerminar = alTerminar
        preparado = true
    }

    override fun reproducir() { reproduciendo = true }
    override fun pausar() { reproduciendo = false }

    override fun liberar() {
        if (preparado) vecesLiberado++
        preparado = false
        reproduciendo = false
        posicionMs = 0
    }

    fun llegarAlFinal() {
        reproduciendo = false
        posicionMs = 0
        alTerminar()
    }
}

// T-411 (US-12): escuchar la grabación, volver a grabar y liberar el reproductor
@OptIn(ExperimentalCoroutinesApi::class)
class ReproduccionViewModelTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var grabadora: GrabadoraDePrueba
    private lateinit var reproductor: ReproductorFalso
    private lateinit var store: ViewModelStore
    private lateinit var viewModel: GrabacionViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        grabadora = GrabadoraDePrueba()
        reproductor = ReproductorFalso()
        store = ViewModelStore()
        val fabrica = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GrabacionViewModel(grabadora, AlmacenGrabaciones(carpeta.root), { 7L }, reproductor) as T
        }
        viewModel = ViewModelProvider(store, fabrica)[GrabacionViewModel::class.java]
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun kotlinx.coroutines.test.TestScope.grabarYTerminar() {
        viewModel.grabar(1)
        grabadora.duracionMs = 20_000
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)
        viewModel.terminar()
    }

    @Test
    fun `sin grabacion no se puede escuchar`() = runTest(dispatcher) {
        viewModel.escuchar()

        assertFalse(reproductor.preparado)
        assertFalse(viewModel.reproduccion.value.reproduciendo)
    }

    @Test
    fun `escucha la grabacion guardada y la barra avanza`() = runTest(dispatcher) {
        grabarYTerminar()
        val grabada = viewModel.uiState.value as GrabacionUiState.Grabada

        viewModel.escuchar()
        runCurrent()
        assertEquals(grabada.grabacion.archivo, reproductor.archivo)
        assertTrue(viewModel.reproduccion.value.reproduciendo)

        reproductor.posicionMs = 5_000
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)
        assertEquals(5_000L, viewModel.reproduccion.value.posicionMs)

        viewModel.pausar()   // apaga el avance; si no, la prueba nunca termina
    }

    @Test
    fun `tocar otra vez pausa donde iba`() = runTest(dispatcher) {
        grabarYTerminar()
        viewModel.escuchar()
        reproductor.posicionMs = 8_000

        viewModel.escuchar()

        assertFalse(reproductor.reproduciendo)
        assertFalse(viewModel.reproduccion.value.reproduciendo)
        assertEquals(8_000L, viewModel.reproduccion.value.posicionMs)
    }

    @Test
    fun `al terminar de escuchar la barra vuelve al inicio`() = runTest(dispatcher) {
        grabarYTerminar()
        viewModel.escuchar()
        reproductor.posicionMs = 19_000
        advanceTimeBy(GrabacionViewModel.INTERVALO_MS + 1)

        reproductor.llegarAlFinal()

        assertEquals(Reproduccion(), viewModel.reproduccion.value)
    }

    @Test
    fun `volver a grabar suelta el reproductor y vuelve al microfono`() = runTest(dispatcher) {
        grabarYTerminar()
        viewModel.escuchar()

        viewModel.grabarOtraVez()

        assertTrue(viewModel.uiState.value is GrabacionUiState.Lista)
        assertFalse(reproductor.preparado)
        assertEquals(1, reproductor.vecesLiberado)
        assertEquals(Reproduccion(), viewModel.reproduccion.value)
    }

    @Test
    fun `la nueva grabacion reemplaza a la anterior al escucharla`() = runTest(dispatcher) {
        grabarYTerminar()
        viewModel.escuchar()
        viewModel.grabarOtraVez()

        grabadora.duracionMs = 0
        grabarYTerminar()
        viewModel.escuchar()

        val grabada = viewModel.uiState.value as GrabacionUiState.Grabada
        assertEquals(20_000L, grabada.grabacion.duracionMs)
        assertEquals(grabada.grabacion.archivo, reproductor.archivo)
        viewModel.pausar()
    }

    @Test
    fun `si el audio no se puede abrir avisa y no queda sonando`() = runTest(dispatcher) {
        grabarYTerminar()
        reproductor.fallar = true

        viewModel.escuchar()

        assertFalse(viewModel.reproduccion.value.reproduciendo)
        assertEquals(GrabacionViewModel.MENSAJE_NO_SE_ESCUCHA, viewModel.reproduccion.value.error)
        assertTrue(viewModel.uiState.value is GrabacionUiState.Grabada)
    }

    @Test
    fun `al salir de la pantalla se libera el reproductor`() = runTest(dispatcher) {
        grabarYTerminar()
        viewModel.escuchar()

        store.clear()

        assertFalse(reproductor.preparado)
        assertEquals(1, reproductor.vecesLiberado)
    }

    @Test
    fun `si la app pasa a segundo plano se pausa`() = runTest(dispatcher) {
        grabarYTerminar()
        viewModel.escuchar()

        viewModel.pausar()

        assertFalse(reproductor.reproduciendo)
        assertTrue(reproductor.preparado)   // al volver puede seguir donde iba
        assertNull(viewModel.reproduccion.value.error)
    }
}
