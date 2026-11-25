package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.proyecto.movil.model.PalabraLeida
import com.proyecto.movil.model.ResultadoEvaluacion
import com.proyecto.movil.viewmodel.EsperaUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// T-428/T-429/T-430 (US-14): palabras por mejorar resaltadas en el resultado y tocar para escuchar
class PalabrasPorMejorarPantallaTest {

    @get:Rule
    val regla = createComposeRule()

    private fun resultado(palabras: List<PalabraLeida>?) = EsperaUiState.Resultado(
        ResultadoEvaluacion(50L, 1, "listo", "", 60, 3, 5, 1, 6, null, null, null, palabras)
    )

    private val conErrores = listOf(
        PalabraLeida("Había", false),
        PalabraLeida("una", false),
        PalabraLeida("vez", false),
        PalabraLeida("un", true),
        PalabraLeida("cóndor.", true)
    )

    @Test
    fun muestraElTextoYLaListaDePalabrasPorMejorar() {
        regla.setContent { ContenidoEspera(resultado(conErrores), null, {}, {}) }

        regla.onNodeWithTag("texto_leido").performScrollTo().assertIsDisplayed()
        regla.onNodeWithTag("palabras_por_mejorar").performScrollTo().assertIsDisplayed()
        regla.onNodeWithTag("chip_cóndor").performScrollTo().assertIsDisplayed()
        regla.onNodeWithTag("chip_un").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun alTocarUnaPalabraDeLaListaSeEscucha() {
        val tocadas = mutableListOf<String>()
        regla.setContent { ContenidoEspera(resultado(conErrores), null, { tocadas += it }, {}) }

        regla.onNodeWithTag("chip_cóndor").performScrollTo().performClick()

        assertEquals(listOf("cóndor"), tocadas)
    }

    @Test
    fun alTocarLaPalabraResaltadaEnElTextoTambienSeEscucha() {
        val tocadas = mutableListOf<String>()
        regla.setContent { ContenidoEspera(resultado(conErrores), null, { tocadas += it }, {}) }

        regla.onNodeWithText("cóndor.").performScrollTo().performClick()

        assertEquals(listOf("cóndor."), tocadas)
    }

    @Test
    fun siLeyoTodoBienLoFelicita() {
        val todasBien = conErrores.map { it.copy(mejorar = false) }
        regla.setContent { ContenidoEspera(resultado(todasBien), null, {}, {}) }

        regla.onNodeWithTag("todas_bien").performScrollTo().assertIsDisplayed()
        regla.onNodeWithTag("palabras_por_mejorar").assertDoesNotExist()
    }

    @Test
    fun sinPalabrasDelBackendSeVeComoAntes() {
        regla.setContent { ContenidoEspera(resultado(null), null, {}, {}) }

        regla.onNodeWithTag("estrellas_resultado").assertIsDisplayed()
        regla.onNodeWithTag("texto_leido").assertDoesNotExist()
    }
}
