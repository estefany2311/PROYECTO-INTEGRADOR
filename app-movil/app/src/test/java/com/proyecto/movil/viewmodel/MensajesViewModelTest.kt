package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.MensajeDocente
import com.proyecto.movil.model.repository.IMensajeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MensajesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val comentario = MensajeDocente(80L, 1, "Mi primera lectura", "audio", "Buen trabajo, sigue así", "2026-11-26T04:30:00")

    // Devuelve en orden las respuestas que se le den (la última se repite)
    // Recibe una lista porque Kotlin no permite vararg de Result
    private class RepositorioFalso(val respuestas: List<Result<List<MensajeDocente>>>) : IMensajeRepository {
        var llamadas = 0
        override suspend fun mensajes(): Result<List<MensajeDocente>> =
            respuestas[minOf(llamadas++, respuestas.lastIndex)]
    }

    @Test
    fun alCargarMuestraLosMensajesDelProfesor() = runTest {
        val vm = MensajesViewModel(RepositorioFalso(listOf(Result.success(listOf(comentario)))))

        vm.cargar()
        advanceUntilIdle()

        assertEquals(listOf(comentario), vm.mensajes.value)
    }

    @Test
    fun sinConexionSeQuedanLosQueYaHabia() = runTest {
        val vm = MensajesViewModel(RepositorioFalso(listOf(Result.success(listOf(comentario)), Result.failure(Exception("sin red")))))

        vm.cargar()
        advanceUntilIdle()
        vm.cargar()
        advanceUntilIdle()

        assertEquals(listOf(comentario), vm.mensajes.value)
    }

    @Test
    fun sinConexionLaPrimeraVezNoHayMensajes() = runTest {
        val vm = MensajesViewModel(RepositorioFalso(listOf(Result.failure(Exception("sin red")))))

        vm.cargar()
        advanceUntilIdle()

        assertEquals(emptyList<MensajeDocente>(), vm.mensajes.value)
    }
}