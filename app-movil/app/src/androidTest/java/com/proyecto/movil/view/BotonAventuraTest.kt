package com.proyecto.movil.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.view.componentes.BotonAventura
import com.proyecto.movil.view.componentes.EstiloBoton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// T-440: el botón con relieve responde al toque y, deshabilitado, no hace nada
class BotonAventuraTest {

    @get:Rule
    val regla = createComposeRule()

    @Test
    fun alTocarloLlamaAlClick() {
        var toques = 0
        regla.setContent { AppMovilTheme { BotonAventura("Continuar", onClick = { toques++ }) } }

        regla.onNodeWithText("Continuar").performClick()

        assertEquals(1, toques)
    }

    @Test
    fun deshabilitadoNoHaceNada() {
        var toques = 0
        regla.setContent {
            AppMovilTheme {
                BotonAventura("Enviar", onClick = { toques++ }, enabled = false,
                    estilo = EstiloBoton.Exito, modifier = Modifier.testTag("boton"))
            }
        }

        regla.onNodeWithTag("boton").assertIsNotEnabled()
        regla.onNodeWithTag("boton").performClick()

        assertEquals(0, toques)
    }
}
