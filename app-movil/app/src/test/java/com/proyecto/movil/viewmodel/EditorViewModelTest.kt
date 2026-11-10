package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.local.AlmacenBorradores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// T-325 (US-18): el editor guarda solo el borrador y lo recupera al volver
@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var almacen: AlmacenBorradores

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        almacen = AlmacenBorradores(carpeta.root)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun nuevoEditor(estudianteId: Long = 7L) =
        EditorViewModel(almacen, estudianteId = { estudianteId }, io = dispatcher)

    @Test
    fun `sin borrador el editor empieza vacio`() = runTest(dispatcher) {
        val vm = nuevoEditor()
        vm.abrir(2)

        assertEquals("", vm.texto.value)
        assertEquals(EstadoGuardado.NADA, vm.guardado.value)
    }

    @Test
    fun `al abrir recupera el borrador del nivel`() = runTest(dispatcher) {
        almacen.guardar(7, 2, "Mi perro se llama Toby.")
        val vm = nuevoEditor()

        vm.abrir(2)

        assertEquals("Mi perro se llama Toby.", vm.texto.value)
        assertEquals(EstadoGuardado.GUARDADO, vm.guardado.value)
    }

    @Test
    fun `mientras escribe dice guardando y no escribe en la tablet con cada letra`() = runTest(dispatcher) {
        val vm = nuevoEditor()
        vm.abrir(2)

        vm.escribir("Mi")
        advanceTimeBy(500)
        vm.escribir("Mi perro")
        advanceTimeBy(500)

        assertEquals(EstadoGuardado.GUARDANDO, vm.guardado.value)
        assertNull(almacen.leer(7, 2))
    }

    @Test
    fun `un segundo despues de dejar de escribir queda guardado`() = runTest(dispatcher) {
        val vm = nuevoEditor()
        vm.abrir(2)

        vm.escribir("Mi perro")
        advanceTimeBy(EditorViewModel.ESPERA_GUARDADO_MS + 1)
        runCurrent()

        assertEquals("Mi perro", almacen.leer(7, 2))
        assertEquals(EstadoGuardado.GUARDADO, vm.guardado.value)
    }

    @Test
    fun `borrar todo el texto borra el borrador`() = runTest(dispatcher) {
        almacen.guardar(7, 2, "Mi perro")
        val vm = nuevoEditor()
        vm.abrir(2)

        vm.escribir("")
        advanceUntilIdle()

        assertNull(almacen.leer(7, 2))
        assertEquals(EstadoGuardado.NADA, vm.guardado.value)
    }

    @Test
    fun `abrir otra vez el mismo nivel no pisa lo que esta escribiendo`() = runTest(dispatcher) {
        almacen.guardar(7, 2, "viejo")
        val vm = nuevoEditor()
        vm.abrir(2)
        vm.escribir("nuevo texto")

        vm.abrir(2)   // como al girar la tablet

        assertEquals("nuevo texto", vm.texto.value)
        advanceUntilIdle()
    }

    @Test
    fun `otro estudiante no ve el borrador`() = runTest(dispatcher) {
        almacen.guardar(7, 2, "de Ana")
        val vm = nuevoEditor(estudianteId = 8L)

        vm.abrir(2)

        assertEquals("", vm.texto.value)
    }

    @Test
    fun `el texto tiene un limite de caracteres`() = runTest(dispatcher) {
        val vm = nuevoEditor()
        vm.abrir(2)

        vm.escribir("a".repeat(EditorViewModel.MAX_CARACTERES + 50))

        assertEquals(EditorViewModel.MAX_CARACTERES, vm.texto.value.length)
        advanceUntilIdle()
    }
}