package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.ResultadoLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * T-235 (US-03): qué ve el niño cuando se equivoca de PIN o su cuenta se bloquea.
 * El tiempo es virtual (StandardTestDispatcher): runCurrent() deja la sacudida en curso
 * y advanceUntilIdle() salta los 600 ms sin esperarlos de verdad.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PinErroresViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var almacen: AlmacenSesionFalso
    private lateinit var viewModel: PinViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        almacen = AlmacenSesionFalso()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Prepara el ViewModel con la respuesta que dará el backend ante un PIN equivocado
    private fun conRespuesta(alFallar: ResultadoLogin) {
        viewModel = PinViewModel(SesionRepositorioFalso(alFallar), almacen).apply { inicializar(1) }
    }

    private fun escribir(pin: String) = pin.forEach { viewModel.presionarDigito(it.digitToInt()) }

    private val estado get() = viewModel.uiState.value

    // ---------- PIN incorrecto ----------

    @Test
    fun `PIN incorrecto muestra cuantos intentos quedan`() = runTest {
        conRespuesta(ResultadoLogin.PinIncorrecto(intentosRestantes = 3))

        escribir("0000")
        advanceUntilIdle()

        assertEquals(PinViewModel.MENSAJE_PIN_INCORRECTO, estado.mensajeError)
        assertEquals("Te quedan 3 intentos.", estado.detalleError)
        assertEquals(TipoError.PIN_INCORRECTO, estado.tipoError)
        assertNull(estado.sesion)
        assertNull(almacen.sesion)
    }

    @Test
    fun `con un solo intento el mensaje va en singular`() = runTest {
        conRespuesta(ResultadoLogin.PinIncorrecto(intentosRestantes = 1))

        escribir("0000")
        advanceUntilIdle()

        assertEquals("Te queda 1 intento.", estado.detalleError)
    }

    @Test
    fun `si el backend no manda los intentos no se inventa un numero`() = runTest {
        conRespuesta(ResultadoLogin.PinIncorrecto(intentosRestantes = null))

        escribir("0000")
        advanceUntilIdle()

        assertEquals(PinViewModel.MENSAJE_PIN_INCORRECTO, estado.mensajeError)
        assertNull(estado.detalleError)
    }

    // T-234: los círculos se quedan llenos mientras se sacuden y después se borran solos
    @Test
    fun `durante la sacudida el PIN sigue lleno y despues se borra solo`() = runTest {
        conRespuesta(ResultadoLogin.PinIncorrecto(intentosRestantes = 3))

        escribir("0000")
        runCurrent()                         // ya respondió el backend, la sacudida empezó

        assertEquals("0000", estado.pin)
        assertTrue(estado.sacudiendo)
        assertEquals(1, estado.sacudidas)

        advanceUntilIdle()                   // pasan los 600 ms

        assertEquals("", estado.pin)
        assertFalse(estado.sacudiendo)
    }

    @Test
    fun `durante la sacudida no se puede escribir ni borrar`() = runTest {
        conRespuesta(ResultadoLogin.PinIncorrecto(intentosRestantes = 3))

        escribir("0000")
        runCurrent()
        viewModel.borrar()
        viewModel.presionarDigito(5)

        assertEquals("0000", estado.pin)
    }

    @Test
    fun `cada error vuelve a sacudir los circulos`() = runTest {
        conRespuesta(ResultadoLogin.PinIncorrecto(intentosRestantes = 3))

        escribir("0000")
        advanceUntilIdle()
        escribir("1111")
        advanceUntilIdle()

        assertEquals(2, estado.sacudidas)
        assertEquals("", estado.pin)
    }

    // ---------- Cuenta bloqueada ----------

    @Test
    fun `cuenta bloqueada muestra cuanto falta en minutos`() = runTest {
        conRespuesta(ResultadoLogin.Bloqueado(segundosRestantes = 120))

        escribir("0000")
        advanceUntilIdle()

        assertEquals(PinViewModel.MENSAJE_BLOQUEADO, estado.mensajeError)
        assertEquals("Podrás intentar otra vez en 2 minutos.", estado.detalleError)
        assertEquals(TipoError.BLOQUEADO, estado.tipoError)
        assertNull(almacen.sesion)
    }

    @Test
    fun `cuenta bloqueada tambien sacude y limpia el PIN`() = runTest {
        conRespuesta(ResultadoLogin.Bloqueado(segundosRestantes = 120))

        escribir("0000")
        runCurrent()
        assertTrue(estado.sacudiendo)

        advanceUntilIdle()
        assertEquals("", estado.pin)
        assertEquals(1, estado.sacudidas)
    }

    @Test
    fun `los segundos se redondean hacia arriba`() {
        assertEquals("Podrás intentar otra vez en 1 minuto.", PinViewModel.textoBloqueo(30))
        assertEquals("Podrás intentar otra vez en 1 minuto.", PinViewModel.textoBloqueo(60))
        assertEquals("Podrás intentar otra vez en 2 minutos.", PinViewModel.textoBloqueo(61))
    }

    @Test
    fun `si el backend no manda el tiempo se dice en un momento`() {
        assertEquals("Podrás intentar otra vez en un momento.", PinViewModel.textoBloqueo(null))
    }

    // ---------- Sin conexión ----------

    // No es culpa del niño: no hay sacudida, el PIN se limpia de inmediato
    @Test
    fun `sin conexion no sacude y limpia el PIN de inmediato`() = runTest {
        conRespuesta(ResultadoLogin.ErrorConexion("timeout"))

        escribir("0000")
        runCurrent()

        assertEquals("", estado.pin)
        assertFalse(estado.sacudiendo)
        assertEquals(0, estado.sacudidas)
        assertEquals(TipoError.SIN_CONEXION, estado.tipoError)
    }
}