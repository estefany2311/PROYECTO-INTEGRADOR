package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-238 (US-04): diálogo "¿Salir?" de T-237.
 * "Sí, salir" cierra la sesión; "No, quedarme" no hace nada más que cerrar el diálogo.
 */
@RunWith(AndroidJUnit4::class)
class DialogoSalirTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var confirmados = 0
    private var cancelados = 0

    private fun abrirDialogo() {
        composeRule.setContent {
            DialogoSalir(
                nombre = "Ana",
                avatarUrl = "",
                onConfirmar = { confirmados++ },
                onCancelar = { cancelados++ }
            )
        }
    }

    @Test
    fun muestraElNombreYElAvatarDelAlumno() {
        abrirDialogo()

        composeRule.onNodeWithText("¿Salir, Ana?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Avatar de Ana").assertExists()
    }

    @Test
    fun siSalirConfirmaUnaSolaVez() {
        abrirDialogo()

        composeRule.onNodeWithTag("boton_confirmar_salir").performClick()

        assertEquals(1, confirmados)
        assertEquals(0, cancelados)
    }

    @Test
    fun noQuedarmeCancelaSinSalir() {
        abrirDialogo()

        composeRule.onNodeWithTag("boton_cancelar_salir").performClick()

        assertEquals(0, confirmados)
        assertEquals(1, cancelados)
    }
}