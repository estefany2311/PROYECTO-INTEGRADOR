package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.model.local.SesionGuardada
import com.proyecto.movil.model.repository.IMapaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * T-212 (US-05): carga del mapa en el ViewModel.
 * El backend se reemplaza por un repositorio falso que devuelve las respuestas en orden.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapaViewModelTest {

    // Devuelve una respuesta por llamada; la última se repite.
    // Lista y no vararg: Kotlin no permite vararg de Result (es una "value class")
    private class MapaRepositorioFalso(respuestas: List<Result<MapaRespuesta>>) : IMapaRepository {
        private val cola = respuestas.toMutableList()
        var llamadas = 0

        override suspend fun obtenerMapa(): Result<MapaRespuesta> {
            llamadas++
            return if (cola.size > 1) cola.removeAt(0) else cola.first()
        }
    }

    private val mapaDeAna = MapaRespuesta(
        estudianteId = 1, nombres = "Ana", apellidos = "Quispe",
        totalNiveles = 6, completados = 1, puntos = 10,
        niveles = (1..6).map { n ->
            NivelMapa(n, n, "Nivel $n", "lectura", null, if (n == 1) "completado" else "bloqueado")
        }
    )

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var almacen: AlmacenSesionFalso

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        almacen = AlmacenSesionFalso().apply {
            guardar(SesionGuardada("token-de-ana", 1, "Ana", ""))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // SesionRepositorioFalso (Falsos.kt): en estas pruebas no se toca "Salir"
    private fun crearViewModel(repo: MapaRepositorioFalso) =
        MapaViewModel(almacen, repo, SesionRepositorioFalso())

    @Test
    fun `al abrir la pantalla empieza cargando`() {
        val viewModel = crearViewModel(MapaRepositorioFalso(listOf(Result.success(mapaDeAna))))

        assertEquals(MapaUiState.Cargando, viewModel.uiState.value)
    }

    @Test
    fun `mientras espera al backend se ve cargando`() = runTest {
        val viewModel = crearViewModel(MapaRepositorioFalso(listOf(Result.success(mapaDeAna))))

        viewModel.cargarMapa()              // todavía no respondió (no se avanzó el tiempo)

        assertEquals(MapaUiState.Cargando, viewModel.uiState.value)
    }

    @Test
    fun `cuando el backend responde se muestra el mapa del alumno`() = runTest {
        val viewModel = crearViewModel(MapaRepositorioFalso(listOf(Result.success(mapaDeAna))))

        viewModel.cargarMapa()
        advanceUntilIdle()

        val estado = viewModel.uiState.value as MapaUiState.Exito
        assertEquals(6, estado.mapa.niveles.size)
        assertEquals(1, estado.mapa.completados)
        assertEquals("Ana", estado.mapa.nombres)
    }

    @Test
    fun `si el backend falla se muestra el mensaje amigable`() = runTest {
        val viewModel = crearViewModel(MapaRepositorioFalso(listOf(Result.failure(Exception("timeout")))))

        viewModel.cargarMapa()
        advanceUntilIdle()

        assertEquals(MapaUiState.Error(MapaViewModel.MENSAJE_ERROR), viewModel.uiState.value)
    }

    // El niño toca "Intentar de nuevo" y esta vez el backend sí responde
    @Test
    fun `intentar de nuevo despues de un error muestra el mapa`() = runTest {
        val repo = MapaRepositorioFalso(listOf(Result.failure(Exception("timeout")), Result.success(mapaDeAna)))
        val viewModel = crearViewModel(repo)

        viewModel.cargarMapa()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is MapaUiState.Error)

        viewModel.cargarMapa()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MapaUiState.Exito)
        assertEquals(2, repo.llamadas)
    }

    // Sin sesión guardada no hay token: no se llama al backend
    @Test
    fun `sin sesion no se pide el mapa`() = runTest {
        almacen.borrar()
        val repo = MapaRepositorioFalso(listOf(Result.success(mapaDeAna)))
        val viewModel = crearViewModel(repo)

        viewModel.cargarMapa()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MapaUiState.Error)
        assertEquals(0, repo.llamadas)
    }
}