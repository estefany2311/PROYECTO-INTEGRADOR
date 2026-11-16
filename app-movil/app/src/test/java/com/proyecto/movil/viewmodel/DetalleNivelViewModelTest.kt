package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.NivelDetalle
import com.proyecto.movil.model.ResultadoNivel
import com.proyecto.movil.model.repository.INivelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * T-243 (US-07): el ViewModel del detalle según lo que responde el backend (T-214).
 * Lo más importante: un nivel bloqueado no se abre, se explica qué terminar antes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleNivelViewModelTest {

    // Devuelve una respuesta por llamada; la última se repite
    private class NivelRepositorioFalso(respuestas: List<ResultadoNivel>) : INivelRepository {
        private val cola = respuestas.toMutableList()
        val pedidos = mutableListOf<Int>()

        override suspend fun obtenerDetalle(nivelId: Int): ResultadoNivel {
            pedidos += nivelId
            return if (cola.size > 1) cola.removeAt(0) else cola.first()
        }
    }

    private val primerosPasos =
        NivelDetalle(1, 1, "Primeros pasos", "lectura", "Lee palabras y oraciones cortas", "desbloqueado")

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun conRespuestas(vararg respuestas: ResultadoNivel) =
        NivelRepositorioFalso(respuestas.toList()).let { it to DetalleNivelViewModel(it) }

    @Test
    fun `mientras espera al backend se ve cargando`() = runTest {
        val (_, viewModel) = conRespuestas(ResultadoNivel.Exito(primerosPasos))

        viewModel.cargar(1)

        assertEquals(DetalleNivelUiState.Cargando, viewModel.uiState.value)
    }

    @Test
    fun `un nivel disponible muestra su detalle`() = runTest {
        val (repo, viewModel) = conRespuestas(ResultadoNivel.Exito(primerosPasos))

        viewModel.cargar(1)
        advanceUntilIdle()

        val estado = viewModel.uiState.value as DetalleNivelUiState.Exito
        assertEquals("Primeros pasos", estado.detalle.nombre)
        assertEquals(listOf(1), repo.pedidos)
    }

    // La regla de T-243
    @Test
    fun `un nivel bloqueado no se abre y dice cual terminar antes`() = runTest {
        val (_, viewModel) = conRespuestas(ResultadoNivel.Bloqueado("Primeros pasos"))

        viewModel.cargar(2)
        advanceUntilIdle()

        assertEquals(
            DetalleNivelUiState.Bloqueado("¡Todavía no! Termina «Primeros pasos» para abrir este nivel."),
            viewModel.uiState.value
        )
    }

    @Test
    fun `un bloqueado sin nivel anterior usa el mensaje general`() = runTest {
        val (_, viewModel) = conRespuestas(ResultadoNivel.Bloqueado(null))

        viewModel.cargar(1)
        advanceUntilIdle()

        assertEquals(DetalleNivelUiState.Bloqueado(MapaViewModel.mensajeNivelBloqueado(null)), viewModel.uiState.value)
    }

    @Test
    fun `cada error muestra su mensaje`() = runTest {
        val casos = mapOf(
            ResultadoNivel.SesionVencida to DetalleNivelViewModel.MENSAJE_SESION_VENCIDA,
            ResultadoNivel.NoEncontrado to DetalleNivelViewModel.MENSAJE_NO_ENCONTRADO,
            ResultadoNivel.ErrorConexion to DetalleNivelViewModel.MENSAJE_SIN_CONEXION
        )
        casos.forEach { (resultado, mensaje) ->
            val (_, viewModel) = conRespuestas(resultado)

            viewModel.cargar(1)
            advanceUntilIdle()

            assertEquals(DetalleNivelUiState.Error(mensaje), viewModel.uiState.value)
        }
    }

    @Test
    fun `reintentar vuelve a pedir el mismo nivel`() = runTest {
        val (repo, viewModel) = conRespuestas(ResultadoNivel.ErrorConexion, ResultadoNivel.Exito(primerosPasos))

        viewModel.cargar(1)
        advanceUntilIdle()
        viewModel.reintentar()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is DetalleNivelUiState.Exito)
        assertEquals(listOf(1, 1), repo.pedidos)
    }

    @Test
    fun `reintentar antes de cargar no pide nada`() = runTest {
        val (repo, viewModel) = conRespuestas(ResultadoNivel.Exito(primerosPasos))

        viewModel.reintentar()
        advanceUntilIdle()

        assertTrue(repo.pedidos.isEmpty())
    }
}