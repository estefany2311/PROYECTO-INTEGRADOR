package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.ResultadoLogin
import com.proyecto.movil.model.local.SesionGuardada
import com.proyecto.movil.model.repository.IMapaRepository
import com.proyecto.movil.model.repository.ISesionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * T-238 (US-04): cierre de sesión desde el mapa.
 * Se comprueba que el token se invalida en el backend, se borra del dispositivo
 * y que la pantalla vuelve a "¿Quién eres?" del mismo salón.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapaSalirViewModelTest {

    // Backend falso para "Salir": anota qué token tenía guardado la tablet al momento de avisar
    private class CierreFalso(
        private val almacen: AlmacenSesionFalso,
        private val respuesta: Boolean = true,
        private val demoraMs: Long = 0
    ) : ISesionRepository {
        var avisos = 0
        var tokenAlAvisar: String? = null

        override suspend fun iniciarSesion(estudianteId: Long, pin: String): ResultadoLogin =
            ResultadoLogin.ErrorConexion("no se usa en estas pruebas")

        override suspend fun cerrarSesion(): Boolean {
            avisos++
            tokenAlAvisar = almacen.leer()?.token
            delay(demoraMs)
            return respuesta
        }
    }

    // El mapa no importa al salir
    private class MapaFalso : IMapaRepository {
        override suspend fun obtenerMapa(): Result<MapaRespuesta> = Result.failure(Exception("sin mapa"))
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var almacen: AlmacenSesionFalso

    // Lo que recibió la pantalla al terminar: el código del aula (o null)
    private var terminado = false
    private var codigoRecibido: String? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        almacen = AlmacenSesionFalso().apply {
            guardar(SesionGuardada("token-de-ana", 1, "Ana", "https://avatar/ana.png"))
            guardarCodigoAula("DEMO123")
        }
        terminado = false
        codigoRecibido = null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(cierre: CierreFalso) = MapaViewModel(almacen, MapaFalso(), cierre)

    private fun salir(viewModel: MapaViewModel) = viewModel.salir { codigo ->
        terminado = true
        codigoRecibido = codigo
    }

    @Test
    fun `salir avisa al backend con el token y despues lo borra del dispositivo`() = runTest {
        val cierre = CierreFalso(almacen)
        val viewModel = crearViewModel(cierre)

        salir(viewModel)
        advanceUntilIdle()

        assertEquals(1, cierre.avisos)
        assertEquals("token-de-ana", cierre.tokenAlAvisar)   // el interceptor todavía lo tenía
        assertNull(almacen.leer())                           // y luego se borró
        assertNull(viewModel.sesion)
    }

    @Test
    fun `salir vuelve a los perfiles del mismo salon`() = runTest {
        salir(crearViewModel(CierreFalso(almacen)))
        advanceUntilIdle()

        assertTrue(terminado)
        assertEquals("DEMO123", codigoRecibido)      // MainActivity navega a perfiles/DEMO123
        assertEquals("DEMO123", almacen.leerCodigoAula())   // el código del aula no se borra
    }

    @Test
    fun `sin codigo de aula guardado se vuelve a pedir el codigo`() = runTest {
        almacen.codigoAula = null

        salir(crearViewModel(CierreFalso(almacen)))
        advanceUntilIdle()

        assertTrue(terminado)
        assertNull(codigoRecibido)                   // MainActivity navega a codigoAula
    }

    // Si el backend responde con error igual se cierra en la tablet
    @Test
    fun `si el backend falla igual se borra el token del dispositivo`() = runTest {
        salir(crearViewModel(CierreFalso(almacen, respuesta = false)))
        advanceUntilIdle()

        assertNull(almacen.leer())
        assertTrue(terminado)
    }

    // Sin conexión: se espera como máximo ESPERA_CIERRE_MS (tiempo virtual, no se espera de verdad)
    @Test
    fun `si el backend no responde se cierra igual despues de la espera maxima`() = runTest {
        salir(crearViewModel(CierreFalso(almacen, demoraMs = 60_000)))

        advanceTimeBy(MapaViewModel.ESPERA_CIERRE_MS - 1)
        runCurrent()
        assertFalse(terminado)                       // todavía esperando al backend
        assertNotNull(almacen.leer())

        advanceTimeBy(2)
        runCurrent()
        assertTrue(terminado)                        // no esperó los 60 s
        assertNull(almacen.leer())
    }
}