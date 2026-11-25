package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.proyecto.movil.model.MejorIntento
import com.proyecto.movil.model.PalabraLeida
import com.proyecto.movil.model.ResultadoEvaluacion
import com.proyecto.movil.viewmodel.EsperaUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// T-433 (US-16): repetir el reto desde el resultado y ver si superó su mejor intento
class IntentarDeNuevoPantallaTest {

    @get:Rule
    val regla = createComposeRule()

    private fun resultado(precision: Int, anterior: MejorIntento?) = EsperaUiState.Resultado(
        ResultadoEvaluacion(
            50L, 4, "listo", "", precision, 4, 5, 2, 8, null, null, null,
            listOf(PalabraLeida("El", false), PalabraLeida("cóndor", true)), anterior
        )
    )

    @Test
    fun conElResultadoPuedeIntentarDeNuevoElMismoNivel() {
        val niveles = mutableListOf<Int>()
        regla.setContent { ContenidoEspera(resultado(80, null), null, {}, {}, { niveles += it }) }

        regla.onNodeWithTag("boton_intentar_de_nuevo").performScrollTo().performClick()

        assertEquals(listOf(4), niveles)
    }

    @Test
    fun siSuperoSuMejorIntentoLoFelicita() {
        regla.setContent { ContenidoEspera(resultado(90, MejorIntento(1, 70, 1)), null, {}, {}, {}) }

        regla.onNodeWithTag("mejor_intento").performScrollTo().assertTextEquals("¡Superaste tu mejor intento!")
    }

    @Test
    fun enSuPrimerIntentoNoMuestraComparacion() {
        regla.setContent { ContenidoEspera(resultado(90, MejorIntento(0, null, null)), null, {}, {}, {}) }

        regla.onNodeWithTag("mejor_intento").assertDoesNotExist()
    }

    @Test
    fun siHuboUnErrorNoOfreceIntentarDeNuevo() {
        regla.setContent { ContenidoEspera(EsperaUiState.Error("No pudimos escuchar bien tu lectura."), null, {}, {}, {}) }

        regla.onNodeWithTag("boton_intentar_de_nuevo").assertDoesNotExist()
        regla.onNodeWithTag("boton_volver_mapa_espera").assertIsDisplayed()
    }
}
