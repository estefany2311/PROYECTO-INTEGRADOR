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
 * T-217 (US-08): el mapa de la tablet después de completar un reto (T-216).
 * Al volver al mapa se refresca y se marcan para animar solo los niveles que se acaban de abrir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapaDesbloqueoViewModelTest {

    // Devuelve una respuesta por llamada; la última se repite
    private class MapaEnOrden(respuestas: List<Result<MapaRespuesta>>) : IMapaRepository {
        private val cola = respuestas.toMutableList()
        override suspend fun obtenerMapa(): Result<MapaRespuesta> = if (cola.size > 1) cola.removeAt(0) else cola.first()
    }

    // Mapa con los estados indicados, en orden de nivel
    private fun mapa(vararg estados: String) = MapaRespuesta(
        estudianteId = 1, nombres = "Ana", apellidos = "Quispe",
        totalNiveles = estados.size, completados = estados.count { it == "completado" }, puntos = 0,
        niveles = estados.mapIndexed { i, e -> NivelMapa(i + 1, i + 1, "Nivel ${i + 1}", "lectura", null, e) }
    )

    private val antes = mapa("desbloqueado", "bloqueado", "bloqueado")
    private val despues = mapa("completado", "desbloqueado", "bloqueado")

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var almacen: AlmacenSesionFalso

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        almacen = AlmacenSesionFalso().apply { guardar(SesionGuardada("token", 1, "Ana", "")) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(vararg respuestas: MapaRespuesta) =
        MapaViewModel(almacen, MapaEnOrden(respuestas.map { Result.success(it) }), SesionRepositorioFalso())

    private fun MapaViewModel.exito() = uiState.value as MapaUiState.Exito

    // ---------- La comparación ----------

    @Test
    fun `detecta el nivel que paso de bloqueado a abierto`() {
        assertEquals(setOf(2), MapaViewModel.desbloqueadosEntre(antes, despues))
    }

    @Test
    fun `en la primera carga no se anima nada`() {
        assertTrue(MapaViewModel.desbloqueadosEntre(null, despues).isEmpty())
    }

    // Completar un nivel que ya estaba abierto no es un desbloqueo
    @Test
    fun `completar un nivel abierto no cuenta como desbloqueo`() {
        assertTrue(MapaViewModel.desbloqueadosEntre(antes, mapa("completado", "bloqueado", "bloqueado")).isEmpty())
    }

    // T-310: al repetir un reto el siguiente ya estaba abierto, no se anima de nuevo
    @Test
    fun `repetir un reto no vuelve a animar el siguiente`() {
        assertTrue(MapaViewModel.desbloqueadosEntre(despues, despues).isEmpty())
    }

    // ---------- El refresco del mapa ----------

    @Test
    fun `al volver al mapa se marca el nivel recien desbloqueado`() = runTest {
        val vm = viewModel(antes, despues)
        vm.cargarMapa(); advanceUntilIdle()
        assertTrue(vm.exito().recienDesbloqueados.isEmpty())

        vm.cargarMapa(); advanceUntilIdle()    // vuelve del reto

        assertEquals(setOf(2), vm.exito().recienDesbloqueados)
        assertEquals(1, vm.exito().mapa.completados)
    }

    // El mapa anterior se queda en pantalla mientras se refresca: no aparece la ruedita
    @Test
    fun `mientras se refresca no se muestra la ruedita`() = runTest {
        val vm = viewModel(antes, despues)
        vm.cargarMapa(); advanceUntilIdle()

        vm.cargarMapa()                          // todavía no respondió

        assertTrue(vm.uiState.value is MapaUiState.Exito)
    }

    @Test
    fun `si el refresco falla se queda el mapa que ya habia`() = runTest {
        val repo = MapaEnOrden(listOf(Result.success(antes), Result.failure(Exception("sin conexion"))))
        val vm = MapaViewModel(almacen, repo, SesionRepositorioFalso())
        vm.cargarMapa(); advanceUntilIdle()

        vm.cargarMapa(); advanceUntilIdle()

        assertEquals(antes, vm.exito().mapa)
    }

    // La animación se muestra una sola vez (no se repite al girar la tablet)
    @Test
    fun `despues de la animacion ya no se vuelve a marcar`() = runTest {
        val vm = viewModel(antes, despues)
        vm.cargarMapa(); advanceUntilIdle()
        vm.cargarMapa(); advanceUntilIdle()

        vm.animacionDesbloqueoMostrada()

        assertTrue(vm.exito().recienDesbloqueados.isEmpty())
        assertEquals(despues, vm.exito().mapa)
    }
}