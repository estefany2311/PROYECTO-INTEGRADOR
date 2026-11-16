package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.ResultadoLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repositorio: SesionRepositorioFalso
    private lateinit var almacen: AlmacenSesionFalso
    private lateinit var viewModel: PinViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repositorio = SesionRepositorioFalso()
        almacen = AlmacenSesionFalso()
        viewModel = crearViewModel(repositorio)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(repo: SesionRepositorioFalso) =
        PinViewModel(repo, almacen).apply { inicializar(estudianteId = 1) }

    // Simula tocar las teclas una por una
    private fun escribir(pin: String) = pin.forEach { viewModel.presionarDigito(it.digitToInt()) }

    @Test
    fun `el estado inicial esta vacio`() {
        val estado = viewModel.uiState.value
        assertEquals("", estado.pin)
        assertFalse(estado.verificando)
        assertNull(estado.mensajeError)
        assertNull(estado.sesion)
    }

    @Test
    fun `al tocar numeros se acumulan en el PIN`() {
        escribir("12")
        assertEquals("12", viewModel.uiState.value.pin)
    }

    @Test
    fun `borrar quita el ultimo numero`() {
        escribir("123")
        viewModel.borrar()
        assertEquals("12", viewModel.uiState.value.pin)
    }

    @Test
    fun `no se aceptan mas de 4 numeros y se verifica una sola vez`() = runTest {
        escribir("98765")
        advanceUntilIdle()
        assertEquals(listOf("9876"), repositorio.pinesRecibidos)
    }

    @Test
    fun `con 3 numeros todavia no se verifica`() = runTest {
        escribir("123")
        advanceUntilIdle()
        assertTrue(repositorio.pinesRecibidos.isEmpty())
    }

    // T-205: el token se guarda en el almacén antes de avisar a la pantalla
    @Test
    fun `PIN correcto guarda el token y deja la sesion en el estado`() = runTest {
        escribir("1234")
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals("token-123", estado.sesion?.token)
        assertEquals("token-123", almacen.sesion?.token)
        assertEquals(1L, almacen.sesion?.estudianteId)
        assertFalse(estado.verificando)
    }

    @Test
    fun `PIN incorrecto limpia el PIN, muestra mensaje y no guarda nada`() = runTest {
        escribir("0000")
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals("", estado.pin)
        assertEquals(PinViewModel.MENSAJE_PIN_INCORRECTO, estado.mensajeError)
        assertNull(estado.sesion)
        assertNull(almacen.sesion)
    }

    @Test
    fun `cuenta bloqueada muestra el mensaje de espera`() = runTest {
        viewModel = crearViewModel(SesionRepositorioFalso(alFallar = ResultadoLogin.Bloqueado(120)))

        escribir("0000")
        advanceUntilIdle()

        assertEquals(PinViewModel.MENSAJE_BLOQUEADO, viewModel.uiState.value.mensajeError)
    }

    @Test
    fun `sin conexion muestra el mensaje para pedir ayuda`() = runTest {
        viewModel = crearViewModel(SesionRepositorioFalso(alFallar = ResultadoLogin.ErrorConexion("timeout")))

        escribir("0000")
        advanceUntilIdle()

        assertEquals(PinViewModel.MENSAJE_SIN_CONEXION, viewModel.uiState.value.mensajeError)
    }

    @Test
    fun `mientras verifica no se puede seguir escribiendo ni borrar`() = runTest {
        escribir("0000")              // la verificación empezó pero todavía no terminó
        viewModel.presionarDigito(5)
        viewModel.borrar()

        assertTrue(viewModel.uiState.value.verificando)
        assertEquals("0000", viewModel.uiState.value.pin)
    }

    @Test
    fun `al volver a escribir despues de un error el mensaje se borra al verificar`() = runTest {
        escribir("0000")
        advanceUntilIdle()
        escribir("1234")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.mensajeError)
        assertNotNull(viewModel.uiState.value.sesion)
    }
}