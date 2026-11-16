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
 * T-241 (US-06): qué pasa al tocar un nivel según su estado (T-213, T-240).
 * Mapa de prueba: 1 completado, 2 desbloqueado, 3 a 6 bloqueados.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NivelBloqueadoViewModelTest {

    private val nombres = listOf("Primeros pasos", "Palabras nuevas", "Cuento corto", "Mi relato", "Leo en voz alta", "Gran final")

    private val niveles = nombres.mapIndexed { i, nombre ->
        val orden = i + 1
        val estado = when (orden) { 1 -> "completado"; 2 -> "desbloqueado"; else -> "bloqueado" }
        NivelMapa(orden, orden, nombre, "lectura", null, estado)
    }

    private fun nivel(orden: Int) = niveles.first { it.orden == orden }

    private class MapaFijo(private val mapa: MapaRespuesta) : IMapaRepository {
        override suspend fun obtenerMapa(): Result<MapaRespuesta> = Result.success(mapa)
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MapaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Carga el mapa; la lista llega desordenada a propósito: el "anterior" se busca por orden
    private fun TestScope.abrirMapa(lista: List<NivelMapa> = niveles.reversed()) {
        val almacen = AlmacenSesionFalso().apply { guardar(SesionGuardada("token", 1, "Ana", "")) }
        viewModel = MapaViewModel(almacen, MapaFijo(MapaRespuesta(1, "Ana", "Quispe", lista.size, 1, 10, lista)), SesionRepositorioFalso())
        viewModel.cargarMapa()
        advanceUntilIdle()
    }

    @Test
    fun `al abrir el mapa no hay ningun mensaje`() = runTest {
        abrirMapa()
        assertNull(viewModel.aviso.value)
    }

    @Test
    fun `tocar un nivel bloqueado dice cual terminar antes`() = runTest {
        abrirMapa()

        viewModel.tocarNivel(nivel(3))

        assertEquals("¡Todavía no! Termina «Palabras nuevas» para abrir este nivel.", viewModel.aviso.value)
    }

    @Test
    fun `el mensaje nombra el nivel justo anterior aunque la lista llegue desordenada`() = runTest {
        abrirMapa()

        viewModel.tocarNivel(nivel(6))

        assertEquals(MapaViewModel.mensajeNivelBloqueado("Leo en voz alta"), viewModel.aviso.value)
    }

    @Test
    fun `tocar un nivel disponible no muestra mensaje`() = runTest {
        abrirMapa()

        viewModel.tocarNivel(nivel(2))

        assertNull(viewModel.aviso.value)
    }

    // Un nivel completado se puede repetir: no es un nivel cerrado
    @Test
    fun `tocar un nivel completado no muestra mensaje`() = runTest {
        abrirMapa()

        viewModel.tocarNivel(nivel(1))

        assertNull(viewModel.aviso.value)
    }

    // Si el primer nivel llegara bloqueado no hay "anterior": se usa el mensaje general
    @Test
    fun `si no hay nivel anterior se usa el mensaje general`() = runTest {
        val primeroBloqueado = listOf(NivelMapa(1, 1, "Primeros pasos", "lectura", null, "bloqueado"))
        abrirMapa(primeroBloqueado)

        viewModel.tocarNivel(primeroBloqueado.first())

        assertEquals("¡Todavía no! Este nivel se abre pronto.", viewModel.aviso.value)
    }

    @Test
    fun `cerrar el aviso lo quita`() = runTest {
        abrirMapa()
        viewModel.tocarNivel(nivel(4))

        viewModel.cerrarAviso()

        assertNull(viewModel.aviso.value)
    }

    @Test
    fun `tocar otro nivel bloqueado cambia el mensaje`() = runTest {
        abrirMapa()

        viewModel.tocarNivel(nivel(3))
        viewModel.tocarNivel(nivel(5))

        assertEquals(MapaViewModel.mensajeNivelBloqueado("Mi relato"), viewModel.aviso.value)
    }
}