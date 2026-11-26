package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.proyecto.movil.model.Perfil
import com.proyecto.movil.model.repository.IPerfilRepository
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.viewmodel.PerfilViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// T-441: pantallas de código de aula y "¿Quién eres?" con el estilo nuevo
class InicioPantallasTest {

    @get:Rule
    val regla = createComposeRule()

    @Test
    fun sinCodigoNoSePuedeContinuar() {
        regla.setContent { AppMovilTheme { PantallaCodigoAula(onContinuar = {}) } }

        regla.onNodeWithTag("boton_continuar_codigo").assertIsNotEnabled()
    }

    @Test
    fun elCodigoSePoneEnMayusculasYContinua() {
        val codigos = mutableListOf<String>()
        regla.setContent { AppMovilTheme { PantallaCodigoAula(onContinuar = { codigos += it }) } }

        regla.onNodeWithTag("campo_codigo_aula").performTextInput("2caull")
        regla.onNodeWithTag("campo_codigo_aula").assertTextContains("2CAULL")
        regla.onNodeWithTag("boton_continuar_codigo").assertIsEnabled().performClick()

        assertEquals(listOf("2CAULL"), codigos)
    }

    private fun conPerfiles(resultado: Result<List<Perfil>>) = PerfilViewModel(object : IPerfilRepository {
        override suspend fun obtenerPerfiles(codigoAula: String) = resultado
    })

    @Test
    fun muestraLosAlumnosYAlTocarUnoLoElige() {
        val ana = Perfil(4, "Ana", "")
        val elegidos = mutableListOf<Perfil>()
        val vm = conPerfiles(Result.success(listOf(ana, Perfil(5, "Bruno", ""))))
        regla.setContent { AppMovilTheme { PantallaPerfiles("2CAULL", onPerfilElegido = { elegidos += it }, viewModel = vm) } }

        regla.onNodeWithText("Aula 2CAULL").assertExists()
        regla.onNodeWithText("Bruno").assertExists()
        regla.onNodeWithTag("perfil_4").performClick()

        assertEquals(listOf(ana), elegidos)
    }

    @Test
    fun siElAulaNoExisteDejaCambiarElCodigo() {
        var cambiar = 0
        val vm = conPerfiles(Result.failure(Exception("Ese código de aula no existe. Revisa el código con tu profesor.")))
        regla.setContent { AppMovilTheme { PantallaPerfiles("XXXX", onPerfilElegido = {}, onCambiarCodigo = { cambiar++ }, viewModel = vm) } }

        regla.onNodeWithText("Ese código de aula no existe", substring = true).assertExists()
        regla.onNodeWithText("Intentar otra vez").assertExists()
        regla.onNodeWithTag("boton_cambiar_codigo").performClick()

        assertEquals(1, cambiar)
    }
}
