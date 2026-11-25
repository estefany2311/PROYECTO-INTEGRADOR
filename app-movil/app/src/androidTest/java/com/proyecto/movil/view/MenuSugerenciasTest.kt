package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.proyecto.movil.model.ErrorOrtografico
import com.proyecto.movil.view.componentes.MenuSugerencias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

// T-366 (US-20): menú de sugerencias al tocar una palabra subrayada
class MenuSugerenciasTest {

    @get:Rule
    val regla = createComposeRule()

    private val kasa = ErrorOrtografico("kasa", 8, 12, "ortografia", "casa", listOf("casa", "caza"))

    @Test
    fun muestraLasSugerenciasYAvisaCualEligio() {
        val elegidas = mutableListOf<String>()
        regla.setContent { MenuSugerencias(kasa, onElegir = { elegidas += it }, onCerrar = {}) }

        regla.onNodeWithTag("titulo_sugerencias").assertTextEquals("¿Quisiste decir…?")
        regla.onNodeWithTag("sugerencia_caza").assertIsDisplayed()
        regla.onNodeWithTag("sugerencia_casa").performClick()

        assertEquals(listOf("casa"), elegidas)
    }

    @Test
    fun sinSugerenciasLePideRevisarLaPalabra() {
        val sinOpciones = ErrorOrtografico("xyz", 0, 3, "ortografia", null, emptyList())
        regla.setContent { MenuSugerencias(sinOpciones, onElegir = {}, onCerrar = {}) }

        regla.onNodeWithTag("titulo_sugerencias").assertTextEquals("Revisa cómo se escribe \"xyz\"")
    }

    @Test
    fun sePuedeCerrar() {
        var cerrado = 0
        regla.setContent { MenuSugerencias(kasa, onElegir = {}, onCerrar = { cerrado++ }) }

        regla.onNodeWithTag("cerrar_sugerencias").performClick()

        assertEquals(1, cerrado)
    }

    @Test
    fun laPalabraTocadaEsLaDelCursor() {
        val texto = "Vivo en kasa grande"
        val errores = listOf(kasa)

        assertEquals(kasa, palabraTocada(TextFieldValue(texto, TextRange(10)), errores))   // en medio de "kasa"
        assertEquals(kasa, palabraTocada(TextFieldValue(texto, TextRange(12)), errores))   // al final de "kasa"
        assertNull(palabraTocada(TextFieldValue(texto, TextRange(2)), errores))           // en "Vivo"
        assertNull(palabraTocada(TextFieldValue(texto, TextRange(8, 12)), errores))       // palabra seleccionada
        assertNull(palabraTocada(TextFieldValue("Vivo en casa grande", TextRange(10)), errores))   // ya la corrigió
    }
}
