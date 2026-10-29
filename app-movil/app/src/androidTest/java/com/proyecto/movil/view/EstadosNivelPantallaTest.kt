package com.proyecto.movil.view

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.viewmodel.MapaUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-241 (US-06): la pantalla distingue los estados de cada nivel (T-239)
 * y avisa qué nivel se tocó (T-240).
 * Mapa de prueba: 1 completado, 2 desbloqueado, 3 a 6 bloqueados.
 */
@RunWith(AndroidJUnit4::class)
class EstadosNivelPantallaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val niveles = (1..6).map { n ->
        val estado = when (n) { 1 -> "completado"; 2 -> "desbloqueado"; else -> "bloqueado" }
        NivelMapa(n, n, "Nivel $n", "lectura", null, estado)
    }

    private val tocados = mutableListOf<Int>()

    private fun abrirMapa() {
        composeRule.setContent {
            ContenidoMapa(
                estado = MapaUiState.Exito(MapaRespuesta(1, "Ana", "Quispe", 6, 1, 10, niveles)),
                onReintentar = {},
                onNivelTocado = { tocados += it.orden }
            )
        }
    }

    // Lo que TalkBack lee después del nombre (T-239)
    private fun conEstado(texto: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, texto)

    @Test
    fun cadaNivelDiceSuEstado() {
        abrirMapa()

        composeRule.onNodeWithTag("nivel_1").assert(conEstado("completado"))
        composeRule.onNodeWithTag("nivel_2").assert(conEstado("disponible"))
        composeRule.onNodeWithTag("nivel_3").assert(conEstado("bloqueado"))
        composeRule.onNodeWithTag("nivel_6").assert(conEstado("bloqueado"))
    }

    @Test
    fun todosLosNivelesSePuedenTocar() {
        abrirMapa()

        (1..6).forEach { n -> composeRule.onNodeWithTag("nivel_$n").assert(hasClickAction()) }
    }

    @Test
    fun tocarUnNivelBloqueadoAvisaCualSeToco() {
        abrirMapa()

        composeRule.onNodeWithTag("nivel_3").performScrollTo().performClick()

        assertEquals(listOf(3), tocados)
    }

    @Test
    fun tocarUnNivelDisponibleTambienAvisa() {
        abrirMapa()

        composeRule.onNodeWithTag("nivel_2").performScrollTo().performClick()

        // La pantalla avisa siempre; el ViewModel decide (T-240: solo los bloqueados muestran mensaje)
        assertEquals(listOf(2), tocados)
    }
}