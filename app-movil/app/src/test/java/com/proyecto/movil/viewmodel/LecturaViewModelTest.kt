package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.LecturaNivel
import com.proyecto.movil.model.ResultadoLectura
import com.proyecto.movil.model.repository.ILecturaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * T-312 (US-10): el ViewModel de la pantalla de lectura (T-219, T-311)
 * según lo que responde el backend (T-218).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LecturaViewModelTest {

    // Devuelve una respuesta por llamada; la última se repite
    private class LecturaRepositorioFalso(respuestas: List<ResultadoLectura>) : ILecturaRepository {
        private val cola = respuestas.toMutableList()
        val pedidos = mutableListOf<Int>()

        override suspend fun obtenerLectura(nivelId: Int): ResultadoLectura {
            pedidos += nivelId
            return if (cola.size > 1) cola.removeAt(0) else cola.first()
        }
    }

    private val condor = LecturaNivel(1, 3, "El cóndor y el zorro", "Había una vez un cóndor…", 2, "basica", false)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun conRespuestas(vararg r: ResultadoLectura): Pair<LecturaRepositorioFalso, LecturaViewModel> {
        val repo = LecturaRepositorioFalso(r.toList())
        return repo to LecturaViewModel(repo)
    }

    // ---------- Cargar la lectura ----------

    @Test
    fun `mientras espera al backend se ve cargando`() = runTest {
        val (_, vm) = conRespuestas(ResultadoLectura.Exito(condor))

        vm.cargar(1)

        assertEquals(LecturaUiState.Cargando, vm.uiState.value)
    }

    @Test
    fun `muestra el texto que tiene que leer`() = runTest {
        val (repo, vm) = conRespuestas(ResultadoLectura.Exito(condor))

        vm.cargar(1)
        advanceUntilIdle()

        assertEquals(LecturaUiState.Exito(condor), vm.uiState.value)
        assertEquals(listOf(1), repo.pedidos)
    }

    @Test
    fun `sin lectura para su grado se avisa y no se ofrece reintentar`() = runTest {
        val (_, vm) = conRespuestas(ResultadoLectura.SinLectura)

        vm.cargar(1)
        advanceUntilIdle()

        assertEquals(LecturaUiState.Error(LecturaViewModel.MENSAJE_SIN_LECTURA, puedeReintentar = false), vm.uiState.value)
    }

    @Test
    fun `un nivel bloqueado dice cual terminar antes`() = runTest {
        val (_, vm) = conRespuestas(ResultadoLectura.Bloqueado("Primeros pasos"))

        vm.cargar(2)
        advanceUntilIdle()

        val error = vm.uiState.value as LecturaUiState.Error
        assertEquals("¡Todavía no! Termina «Primeros pasos» para abrir este nivel.", error.mensaje)
        assertFalse(error.puedeReintentar)
    }

    @Test
    fun `sin conexion se puede reintentar y la segunda vez carga`() = runTest {
        val (repo, vm) = conRespuestas(ResultadoLectura.ErrorConexion, ResultadoLectura.Exito(condor))

        vm.cargar(1)
        advanceUntilIdle()
        assertEquals(LecturaUiState.Error(LecturaViewModel.MENSAJE_SIN_CONEXION, puedeReintentar = true), vm.uiState.value)

        vm.reintentar()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is LecturaUiState.Exito)
        assertEquals(listOf(1, 1), repo.pedidos)
    }

    @Test
    fun `con la sesion vencida se pide ayuda`() = runTest {
        val (_, vm) = conRespuestas(ResultadoLectura.SesionVencida)

        vm.cargar(1)
        advanceUntilIdle()

        assertEquals(LecturaUiState.Error(LecturaViewModel.MENSAJE_SESION_VENCIDA, puedeReintentar = false), vm.uiState.value)
    }

    // ---------- Tamaño de letra (T-311) ----------

    @Test
    fun `empieza en 32 sp`() {
        val (_, vm) = conRespuestas(ResultadoLectura.Exito(condor))

        assertEquals(32, LecturaViewModel.TAMANOS_LETRA[vm.indiceTamano.value])
    }

    @Test
    fun `agrandar sube de a un tamano y se detiene en 44 sp`() {
        val (_, vm) = conRespuestas(ResultadoLectura.Exito(condor))

        repeat(10) { vm.agrandarLetra() }

        assertEquals(44, LecturaViewModel.TAMANOS_LETRA[vm.indiceTamano.value])
    }

    @Test
    fun `achicar baja de a un tamano y se detiene en 24 sp`() {
        val (_, vm) = conRespuestas(ResultadoLectura.Exito(condor))

        vm.achicarLetra()
        assertEquals(28, LecturaViewModel.TAMANOS_LETRA[vm.indiceTamano.value])

        repeat(10) { vm.achicarLetra() }
        assertEquals(24, LecturaViewModel.TAMANOS_LETRA[vm.indiceTamano.value])
    }
}