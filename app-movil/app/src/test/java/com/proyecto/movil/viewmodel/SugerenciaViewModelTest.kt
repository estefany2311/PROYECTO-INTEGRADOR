package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.ErrorOrtografico
import com.proyecto.movil.model.ResultadoEnvioTexto
import com.proyecto.movil.model.ResultadoOrtografia
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// T-367 (US-20): al elegir una sugerencia el texto cambia y el error desaparece
@OptIn(ExperimentalCoroutinesApi::class)
class SugerenciaViewModelTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val texto = "Vivo en kasa con mi perro jugeton."
    private val kasa = ErrorOrtografico("kasa", 8, 12, "ortografia", "casa", listOf("casa", "caza"))
    private val jugeton = ErrorOrtografico("jugeton", 26, 33, "tilde", "juguetón")

    // El corrector marca los dos errores del texto original; después ya no encuentra nada
    private val ortografia = object : IOrtografiaRepository {
        override suspend fun verificar(texto: String) =
            ResultadoOrtografia.Exito(if (texto == this@SugerenciaViewModelTest.texto) listOf(kasa, jugeton) else emptyList())
    }
    private val red = object : IMonitorRed {
        override val conectado = MutableStateFlow(true)
    }
    private val textos = object : ITextoRepository {
        override suspend fun enviar(nivelId: Int, texto: String) = ResultadoEnvioTexto.SinConexion
    }

    private lateinit var almacen: AlmacenBorradores
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
    fun cambiaLaPalabraYLosOtrosErroresSiguenEnSuLugar() = runTest(dispatcher) {
        viewModel.escribir(texto)
        advanceUntilIdle()
        assertEquals(listOf(kasa, jugeton), viewModel.errores.value)

        viewModel.elegirSugerencia(kasa, "casa")

        assertEquals("Vivo en casa con mi perro jugeton.", viewModel.texto.value)
        // "casa" tiene el mismo largo que "kasa": jugeton sigue marcado en el mismo lugar
        assertEquals(listOf(jugeton), viewModel.errores.value)
    }

    @Test
    fun conUnaSugerenciaMasLargaCorreLosErroresQueSiguen() = runTest(dispatcher) {
        viewModel.escribir(texto)
        advanceUntilIdle()

        viewModel.elegirSugerencia(kasa, "casita")

        assertEquals("Vivo en casita con mi perro jugeton.", viewModel.texto.value)
        assertEquals(listOf(jugeton.copy(inicio = 28, fin = 35)), viewModel.errores.value)
    }

    @Test
    fun laCorreccionSeGuardaEnLaTablet() = runTest(dispatcher) {
        viewModel.escribir(texto)
        advanceUntilIdle()

        viewModel.elegirSugerencia(kasa, "casa")
        advanceUntilIdle()

        assertEquals("Vivo en casa con mi perro jugeton.", almacen.leer(7L, 2))
    }

    @Test
    fun siLaPalabraYaCambioNoHaceNada() = runTest(dispatcher) {
        viewModel.escribir("Vivo en casa con mi perro.")

        viewModel.elegirSugerencia(kasa, "casa")

        assertEquals("Vivo en casa con mi perro.", viewModel.texto.value)
    }
}
