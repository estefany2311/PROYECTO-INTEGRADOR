package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.Perfil
import com.proyecto.movil.model.repository.IPerfilRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Repositorio falso que simula una respuesta exitosa
    private class RepositorioFalsoExitoso : IPerfilRepository {
        override suspend fun obtenerPerfiles(codigoAula: String): Result<List<Perfil>> {
            return Result.success(
                listOf(
                    Perfil(1, "Ana", "https://robohash.org/Ana.png"),
                    Perfil(2, "Luis", "https://robohash.org/Luis.png")
                )
            )
        }
    }

    // Repositorio falso que simula un error de conexión
    private class RepositorioFalsoConError : IPerfilRepository {
        override suspend fun obtenerPerfiles(codigoAula: String): Result<List<Perfil>> {
            return Result.failure(Exception("Sin conexión"))
        }
    }

    @Test
    fun `el estado inicial es Cargando`() {
        val viewModel = PerfilViewModel(RepositorioFalsoExitoso())
        assertTrue(viewModel.uiState.value is PerfilesUiState.Cargando)
    }

    @Test
    fun `cuando el repositorio responde bien, el estado pasa a Exito con los perfiles`() = runTest {
        val viewModel = PerfilViewModel(RepositorioFalsoExitoso())

        viewModel.cargarPerfiles("DEMO123")
        advanceUntilIdle() // espera a que la corrutina termine

        val estado = viewModel.uiState.value
        assertTrue(estado is PerfilesUiState.Exito)
        assertEquals(2, (estado as PerfilesUiState.Exito).perfiles.size)
        assertEquals("Ana", estado.perfiles[0].nombre)
    }

    @Test
    fun `cuando el repositorio falla, el estado pasa a Error`() = runTest {
        val viewModel = PerfilViewModel(RepositorioFalsoConError())

        viewModel.cargarPerfiles("CODIGO_INVALIDO")
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertTrue(estado is PerfilesUiState.Error)
        assertEquals("Sin conexión", (estado as PerfilesUiState.Error).mensaje)
    }
}