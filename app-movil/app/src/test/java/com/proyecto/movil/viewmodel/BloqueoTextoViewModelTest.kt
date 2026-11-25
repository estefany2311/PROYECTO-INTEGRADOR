package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.ErrorOrtografico
import com.proyecto.movil.model.ResultadoEnvioTexto
import com.proyecto.movil.model.ResultadoOrtografia
import com.proyecto.movil.model.TextoRecibido
import com.proyecto.movil.model.local.AlmacenBorradores
import com.proyecto.movil.model.remote.IMonitorRed
import com.proyecto.movil.model.repository.IOrtografiaRepository
import com.proyecto.movil.model.repository.ITextoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// T-438 (US-21): después de enviar no se puede editar (T-436) y lo que quedó en cola se envía al volver (T-437)
@OptIn(ExperimentalCoroutinesApi::class)
class BloqueoTextoViewModelTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val texto = "Mi perro se llama Toby."

    private class TextosFalsos : ITextoRepository {
        var respuesta: ResultadoEnvioTexto = ResultadoEnvioTexto.Exito(TextoRecibido(80L, 2, 5, emptyList()), null, null)
        val enviados = mutableListOf<String>()
        override suspend fun enviar(nivelId: Int, texto: String): ResultadoEnvioTexto {
            enviados += texto
            return respuesta
        }
    }

    private val ortografia = object : IOrtografiaRepository {
        override suspend fun verificar(texto: String) = ResultadoOrtografia.Exito(emptyList())
    }
    private val red = object : IMonitorRed {
        override val conectado = MutableStateFlow(true)
    }
    private val textos = TextosFalsos()
    private lateinit var almacen: AlmacenBorradores

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        almacen = AlmacenBorradores(carpeta.newFolder("borradores"))
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun nuevoEditor() = EditorViewModel(almacen, { 7L }, dispatcher, ortografia, red, textos).also { it.abrir(2) }

    @Test
    fun despuesDeEnviarNoSePuedeEscribirNiCambiarPalabras() = runTest(dispatcher) {
        val editor = nuevoEditor()
        editor.escribir(texto)
        editor.pedirEnviar()
        editor.confirmarEnvio()
        advanceUntilIdle()

        editor.escribir("Otro texto")
        editor.elegirSugerencia(ErrorOrtografico("perro", 3, 8, "ortografia", "pero"), "pero")
        editor.pedirEnviar()

        assertEquals(texto, editor.texto.value)
        assertTrue(editor.envio.value is EnvioTextoUiState.Enviado)
        assertEquals(1, textos.enviados.size)
    }

    @Test
    fun alVolverAlNivelVeSuTextoBloqueado() = runTest(dispatcher) {
        val primero = nuevoEditor()
        primero.escribir(texto)
        primero.pedirEnviar()
        primero.confirmarEnvio()
        advanceUntilIdle()

        val otraVez = nuevoEditor()   // sale y vuelve a entrar al nivel

        assertEquals(EnvioTextoUiState.YaEnviado(texto), otraVez.envio.value)
        assertEquals(texto, otraVez.texto.value)
        otraVez.escribir("Quiero cambiarlo")
        assertEquals(texto, otraVez.texto.value)
    }

    @Test
    fun loQueQuedoEnColaSeEnviaAlVolverAAbrirElNivel() = runTest(dispatcher) {
        red.conectado.value = false
        val primero = nuevoEditor()
        primero.escribir(texto)
        primero.pedirEnviar()
        primero.confirmarEnvio()
        advanceUntilIdle()
        assertEquals(EnvioTextoUiState.EnCola, primero.envio.value)
        assertTrue(textos.enviados.isEmpty())

        red.conectado.value = true
        val otraVez = nuevoEditor()   // cerró la app sin internet y la volvió a abrir con internet
        advanceUntilIdle()

        assertTrue(textos.enviados.isNotEmpty())
        assertTrue(otraVez.envio.value is EnvioTextoUiState.Enviado)
    }

    @Test
    fun enColaNoSePuedeCambiarElTexto() = runTest(dispatcher) {
        red.conectado.value = false
        val editor = nuevoEditor()
        editor.escribir(texto)
        editor.pedirEnviar()
        editor.confirmarEnvio()
        advanceUntilIdle()

        editor.escribir("Otro texto")

        assertEquals(texto, editor.texto.value)
    }
}
