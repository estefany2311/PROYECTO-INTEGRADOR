package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.model.ResultadoConsigna
import com.proyecto.movil.model.repository.IConsignaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * T-322 (US-17): el ViewModel de la pantalla de consigna (T-321)
 * según lo que responde el backend (T-320).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConsignaViewModelTest {

    // Devuelve una respuesta por llamada; la última se repite
    private class ConsignaRepositorioFalso(respuestas: List<ResultadoConsigna>) : IConsignaRepository {
        private val cola = respuestas.toMutableList()
        val pedidos = mutableListOf<Int>()

        override suspend fun obtenerConsigna(nivelId: Int): ResultadoConsigna {
            pedidos += nivelId
            return if (cola.size > 1) cola.removeAt(0) else cola.first()
        }
    }

    private val animal = ConsignaNivel(
        2, "Palabras nuevas", "Escribe tres oraciones sobre tu animal favorito.",
        "Cuenta cómo es, qué come y dónde vive.", "animal", 15
    )

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun conRespuestas(vararg r: ResultadoConsigna): Pair<ConsignaRepositorioFalso, ConsignaViewModel> {
        val repo = ConsignaRepositorioFalso(r.toList())
        return repo to ConsignaViewModel(repo)
    }

    @Test
    fun `mientras espera al backend se ve cargando`() = runTest {
        val (_, vm) = conRespuestas(ResultadoConsigna.Exito(animal))

        vm.cargar(2)

        assertEquals(ConsignaUiState.Cargando, vm.uiState.value)
    }

    @Test
    fun `muestra la consigna del nivel que se pidio`() = runTest {
        val (repo, vm) = conRespuestas(ResultadoConsigna.Exito(animal))

        vm.cargar(2)
        advanceUntilIdle()

        assertEquals(ConsignaUiState.Exito(animal), vm.uiState.value)
        assertEquals(listOf(2), repo.pedidos)
    }

    @Test
    fun `nivel bloqueado dice que nivel terminar y no deja reintentar`() = runTest {
        val (_, vm) = conRespuestas(ResultadoConsigna.Bloqueado("Primeros pasos"))

        vm.cargar(5)
        advanceUntilIdle()

        val estado = vm.uiState.value as ConsignaUiState.Error
        assertEquals("¡Todavía no! Termina «Primeros pasos» para abrir este nivel.", estado.mensaje)
        assertFalse(estado.puedeReintentar)
    }

    @Test
    fun `nivel sin consigna avisa que el profesor no la preparo`() = runTest {
        val (_, vm) = conRespuestas(ResultadoConsigna.SinConsigna)

        vm.cargar(5)
        advanceUntilIdle()

        val estado = vm.uiState.value as ConsignaUiState.Error
        assertEquals(ConsignaViewModel.MENSAJE_SIN_CONSIGNA, estado.mensaje)
        assertFalse(estado.puedeReintentar)
    }

    @Test
    fun `sesion vencida no deja reintentar`() = runTest {
        val (_, vm) = conRespuestas(ResultadoConsigna.SesionVencida)

        vm.cargar(2)
        advanceUntilIdle()

        val estado = vm.uiState.value as ConsignaUiState.Error
        assertEquals(ConsignaViewModel.MENSAJE_SESION_VENCIDA, estado.mensaje)
        assertFalse(estado.puedeReintentar)
    }

    @Test
    fun `sin conexion deja reintentar y al reintentar pide el mismo nivel`() = runTest {
        val (repo, vm) = conRespuestas(ResultadoConsigna.ErrorConexion, ResultadoConsigna.Exito(animal))

        vm.cargar(2)
        advanceUntilIdle()
        val error = vm.uiState.value as ConsignaUiState.Error
        assertTrue(error.puedeReintentar)

        vm.reintentar()
        advanceUntilIdle()

        assertEquals(ConsignaUiState.Exito(animal), vm.uiState.value)
        assertEquals(listOf(2, 2), repo.pedidos)
    }

    @Test
    fun `reintentar antes de cargar no hace nada`() = runTest {
        val (repo, vm) = conRespuestas(ResultadoConsigna.Exito(animal))

        vm.reintentar()
        advanceUntilIdle()

        assertEquals(ConsignaUiState.Cargando, vm.uiState.value)
        assertTrue(repo.pedidos.isEmpty())
    }
}