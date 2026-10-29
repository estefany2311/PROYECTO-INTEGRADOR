package com.proyecto.movil.view
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proyecto.movil.model.ResultadoLogin
import com.proyecto.movil.model.SesionRespuesta
import com.proyecto.movil.model.local.IAlmacenSesion
import com.proyecto.movil.model.local.SesionGuardada
import com.proyecto.movil.model.repository.ISesionRepository
import com.proyecto.movil.viewmodel.PinViewModel
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


// T-206: prueba del flujo tocando la pantalla de verdad (Compose UI Test usa Espresso por debajo)
@RunWith(AndroidJUnit4::class)
class PantallaPinTest {

    @get:Rule
    val composeRule = createComposeRule()

    // PIN correcto: 1234. Cualquier otro: incorrecto
    private val repositorio = object : ISesionRepository {
        override suspend fun iniciarSesion(estudianteId: Long, pin: String): ResultadoLogin =
            if (pin == "1234") ResultadoLogin.Exito(SesionRespuesta("token", estudianteId, "Ana", ""))
            else ResultadoLogin.PinIncorrecto(intentosRestantes = 4)
    }

    private val almacen = object : IAlmacenSesion {
        var guardada: SesionGuardada? = null
        override fun guardar(sesion: SesionGuardada) { guardada = sesion }
        override fun leer() = guardada
        override fun borrar() { guardada = null }
        override fun guardarCodigoAula(codigo: String) {}
        override fun leerCodigoAula(): String? = null
    }

    private fun abrirPantalla(onSesion: (SesionRespuesta) -> Unit = {}) {
        composeRule.setContent {
            PantallaPin(
                estudianteId = 1,
                nombre = "Ana",
                avatarUrl = "",
                onVolver = {},
                onSesionIniciada = onSesion,
                viewModel = remember { PinViewModel(repositorio, almacen) }
            )
        }
    }

    private fun tocar(pin: String) = pin.forEach { composeRule.onNodeWithTag("tecla_$it").performClick() }

    @Test
    fun muestraElNombreYElTeclado() {
        abrirPantalla()

        composeRule.onNodeWithText("Hola, Ana").assertIsDisplayed()
        composeRule.onNodeWithTag("tecla_1").assertIsDisplayed()
        composeRule.onNodeWithTag("tecla_borrar").assertIsDisplayed()
    }

    @Test
    fun losCirculosCuentanLosNumerosYBorrarQuitaUno() {
        abrirPantalla()

        tocar("12")
        composeRule.onNodeWithContentDescription("2 de 4 números escritos").assertExists()

        composeRule.onNodeWithTag("tecla_borrar").performClick()
        composeRule.onNodeWithContentDescription("1 de 4 números escritos").assertExists()
    }

    @Test
    fun pinCorrectoIniciaSesionYGuardaElToken() {
        var sesion: SesionRespuesta? = null
        abrirPantalla { sesion = it }

        tocar("1234")

        composeRule.waitUntil(timeoutMillis = 3_000) { sesion != null }
        assertEquals("token", sesion?.token)
        assertEquals("token", almacen.guardada?.token)
    }

    @Test
    fun pinIncorrectoMuestraElMensajeYLimpiaLosCirculos() {
        abrirPantalla()

        tocar("0000")

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(PinViewModel.MENSAJE_PIN_INCORRECTO).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(PinViewModel.MENSAJE_PIN_INCORRECTO).assertIsDisplayed()
        // T-234: los círculos se borran solos cuando termina la sacudida (unos 600 ms)
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithContentDescription("0 de 4 números escritos").fetchSemanticsNodes().isNotEmpty()
        }
    }
}