package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.EstadoEvidencia
import com.proyecto.movil.model.ResultadoEstado
import com.proyecto.movil.model.repository.IEvidenciaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// Devuelve las respuestas en orden; cuando se acaban repite la última
private class EvidenciasFalsas(vararg respuestas: ResultadoEstado) : IEvidenciaRepository {
    private val cola = respuestas.toMutableList()
    var consultas = 0
    override suspend fun estado(evidenciaId: Long): ResultadoEstado {
        consultas++
        return if (cola.size > 1) cola.removeAt(0) else cola.first()
    }
}

private fun estado(estado: String, transcripcion: String? = null) =
    ResultadoEstado.Exito(EstadoEvidencia(50L, estado, transcripcion, null))

// T-418 (US-13): la pantalla de espera pregunta hasta que la IA termina
@OptIn(ExperimentalCoroutinesApi::class)
class EsperaViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun `mientras procesa sigue escuchando`() = runTest(dispatcher) {
        val falsas = EvidenciasFalsas(estado("procesando"))
        val vm = EsperaViewModel(falsas)

        vm.seguir(50L)
        runCurrent()
        advanceTimeBy(EsperaViewModel.INTERVALO_MS * 3 + 1)

        assertEquals(EsperaUiState.Escuchando, vm.uiState.value)
        assertEquals(4, falsas.consultas)
        advanceUntilIdle()   // termina por tiempo máximo y la prueba no queda colgada
    }

    @Test
    fun `cuando la IA termina muestra lo que escucho`() = runTest(dispatcher) {
        val vm = EsperaViewModel(EvidenciasFalsas(estado("procesando"), estado("procesando"), estado("listo", "Había una vez")))

        vm.seguir(50L)
        advanceUntilIdle()

        assertEquals(EsperaUiState.Lista("Había una vez"), vm.uiState.value)
    }

    @Test
    fun `si la IA falla avisa sin culpar al nino`() = runTest(dispatcher) {
        val vm = EsperaViewModel(EvidenciasFalsas(estado("error")))

        vm.seguir(50L)
        advanceUntilIdle()

        assertEquals(EsperaUiState.Error(EsperaViewModel.MENSAJE_ERROR_IA), vm.uiState.value)
    }

    @Test
    fun `sin internet un rato sigue preguntando`() = runTest(dispatcher) {
        val vm = EsperaViewModel(EvidenciasFalsas(ResultadoEstado.SinConexion, ResultadoEstado.SinConexion, estado("listo", "Hola")))

        vm.seguir(50L)
        advanceUntilIdle()

        assertEquals(EsperaUiState.Lista("Hola"), vm.uiState.value)
    }

    @Test
    fun `si pasa el tiempo maximo dice que tarda mucho`() = runTest(dispatcher) {
        val falsas = EvidenciasFalsas(estado("procesando"))
        val vm = EsperaViewModel(falsas)

        vm.seguir(50L)
        advanceUntilIdle()

        assertEquals(EsperaUiState.TardaMucho, vm.uiState.value)
        assertEquals((EsperaViewModel.ESPERA_MAXIMA_MS / EsperaViewModel.INTERVALO_MS + 1).toInt(), falsas.consultas)
    }

    @Test
    fun `con la sesion vencida avisa`() = runTest(dispatcher) {
        val vm = EsperaViewModel(EvidenciasFalsas(ResultadoEstado.SesionVencida))

        vm.seguir(50L)
        advanceUntilIdle()

        assertEquals(EsperaUiState.Error(EsperaViewModel.MENSAJE_SESION_VENCIDA), vm.uiState.value)
    }

    @Test
    fun `llamar dos veces no duplica las consultas`() = runTest(dispatcher) {
        val falsas = EvidenciasFalsas(estado("listo", "Hola"))
        val vm = EsperaViewModel(falsas)

        vm.seguir(50L)
        vm.seguir(50L)
        advanceUntilIdle()

        assertEquals(1, falsas.consultas)
    }
}
