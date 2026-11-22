package com.proyecto.movil.view

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.model.local.AlmacenBorradores
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.viewmodel.ConsignaUiState
import com.proyecto.movil.viewmodel.EditorViewModel
import com.proyecto.movil.viewmodel.EstadoGuardado
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

// T-326 (US-18): pruebas del editor en la tablet: contador, girar la pantalla, salir y volver
class PantallaEditorTest {

    @get:Rule
    val compose = createComposeRule()

    private val consigna = ConsignaNivel(
        2, "Palabras nuevas", "Escribe tres oraciones sobre tu animal favorito.",
        "Cuenta cómo es, qué come y dónde vive.", "animal", 5
    )
    private val cincoPalabras = "Mi perro se llama Toby"

    private lateinit var carpeta: File
    private lateinit var almacen: AlmacenBorradores

    @Before
    fun preparar() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        carpeta = File(contexto.cacheDir, "borradores_prueba").apply { deleteRecursively() }
        almacen = AlmacenBorradores(carpeta)
    }

    @After
    fun limpiar() {
        carpeta.deleteRecursively()
    }

    // Crea el ViewModel dentro de un "store", como lo hace la app con cada pantalla
    private fun editorEn(store: ViewModelStore): EditorViewModel {
        val fabrica = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditorViewModel(almacen, estudianteId = { 7L }) as T
        }
        return ViewModelProvider(store, fabrica)[EditorViewModel::class.java]
    }

    private fun mostrarCon(vm: EditorViewModel, onEnviar: () -> Unit = {}) = compose.setContent {
        val texto by vm.texto.collectAsState()
        val guardado by vm.guardado.collectAsState()
        AppMovilTheme(dynamicColor = false) {
            ContenidoEditor(ConsignaUiState.Exito(consigna), texto, vm::escribir, {}, onEnviar, {}, guardado)
        }
    }

    // ---------- Pantalla ----------

    @Test
    fun muestraLaConsignaYLaAyudaCuandoEstaVacio() {
        val vm = editorEn(ViewModelStore()).also { it.abrir(2) }
        mostrarCon(vm)

        compose.onNodeWithTag("consigna_editor").assertTextContains("Escribe tres oraciones sobre tu animal favorito.")
        compose.onNodeWithTag("ayuda_campo_texto", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("contador_palabras").assertTextContains("0 de 5 palabras")
    }

    @Test
    fun alEscribirCuentaLasPalabrasYDiceCuantasFaltan() {
        val vm = editorEn(ViewModelStore()).also { it.abrir(2) }
        mostrarCon(vm)

        compose.onNodeWithTag("campo_texto").performTextInput("Mi perro")

        compose.onNodeWithTag("contador_palabras").assertTextContains("2 de 5 palabras")
        compose.onNodeWithTag("aviso_palabras").assertTextContains("Te faltan 3 palabras")
        compose.onNodeWithTag("ayuda_campo_texto", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun enviarSeActivaRecienAlLlegarAlMinimo() {
        var enviado = false
        val vm = editorEn(ViewModelStore()).also { it.abrir(2) }
        mostrarCon(vm) { enviado = true }

        compose.onNodeWithTag("campo_texto").performTextInput("Mi perro se llama")
        compose.onNodeWithTag("boton_enviar_texto").assertIsNotEnabled()

        compose.onNodeWithTag("campo_texto").performTextInput(" Toby")
        compose.onNodeWithTag("aviso_palabras").assertTextContains("¡Llegaste al mínimo!", substring = true)
        compose.onNodeWithTag("boton_enviar_texto").assertIsEnabled().performClick()

        assertTrue(enviado)
    }

    @Test
    fun mientrasEscribeDiceGuardandoYLuegoGuardado() {
        val vm = editorEn(ViewModelStore()).also { it.abrir(2) }
        mostrarCon(vm)

        compose.onNodeWithTag("campo_texto").performTextInput("Mi perro")
        compose.onNodeWithText("Guardando…").assertIsDisplayed()

        compose.waitUntil(timeoutMillis = 4_000) { vm.guardado.value == EstadoGuardado.GUARDADO }
        compose.onNodeWithText("Guardado").assertIsDisplayed()
        assertEquals("Mi perro", almacen.leer(7, 2))
    }

    // ---------- Girar la pantalla ----------

    // Al girar la tablet Android vuelve a dibujar la pantalla, pero el ViewModel sigue vivo
    @Test
    fun alGirarLaPantallaElTextoSigue() {
        val vm = editorEn(ViewModelStore()).also { it.abrir(2) }
        val giro = StateRestorationTester(compose)
        giro.setContent {
            val texto by vm.texto.collectAsState()
            AppMovilTheme(dynamicColor = false) {
                ContenidoEditor(ConsignaUiState.Exito(consigna), texto, vm::escribir, {}, {}, {})
            }
        }
        compose.onNodeWithTag("campo_texto").performTextInput(cincoPalabras)

        giro.emulateSavedInstanceStateRestore()
        vm.abrir(2)   // la pantalla vuelve a llamar a abrir() al dibujarse otra vez

        compose.onNodeWithTag("campo_texto").assertTextContains(cincoPalabras)
        compose.onNodeWithTag("contador_palabras").assertTextContains("5 de 5 palabras")
    }

    // ---------- Salir y volver ----------

    @Test
    fun alSalirYVolverAlNivelRecuperaElTexto() {
        val primeraVez = ViewModelStore()
        val vm = editorEn(primeraVez).also { it.abrir(2) }
        mostrarCon(vm)
        compose.onNodeWithTag("campo_texto").performTextInput(cincoPalabras)
        compose.waitUntil(timeoutMillis = 4_000) { almacen.leer(7, 2) != null }
        primeraVez.clear()                       // el niño toca "Volver": la pantalla se destruye

        val otraVez = editorEn(ViewModelStore()).also { it.abrir(2) }

        assertEquals(cincoPalabras, otraVez.texto.value)
        assertEquals(EstadoGuardado.GUARDADO, otraVez.guardado.value)
    }

    // Si sale antes de que pase el segundo de espera, igual se guarda al salir
    @Test
    fun siSaleEnseguidaIgualSeGuarda() {
        val store = ViewModelStore()
        val vm = editorEn(store)
        compose.runOnIdle {
            vm.abrir(2)
            vm.escribir("Mi perro Toby")
            store.clear()                         // sale antes de 1 segundo
        }

        assertEquals("Mi perro Toby", almacen.leer(7, 2))
    }

    @Test
    fun otroAlumnoEnLaMismaTabletNoVeElBorrador() {
        almacen.guardar(7, 2, "Texto de Ana")
        val fabrica = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditorViewModel(almacen, estudianteId = { 8L }) as T
        }
        val deLuis = ViewModelProvider(ViewModelStore(), fabrica)[EditorViewModel::class.java]

        compose.runOnIdle { deLuis.abrir(2) }

        assertEquals("", deLuis.texto.value)
    }
}