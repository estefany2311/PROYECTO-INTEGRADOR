package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proyecto.movil.model.NivelDetalle
import com.proyecto.movil.viewmodel.DetalleNivelUiState
import com.proyecto.movil.viewmodel.MapaViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-243 (US-07): la pantalla de detalle (T-215, T-242).
 * Un nivel bloqueado no muestra el botón "Comenzar".
 */
@RunWith(AndroidJUnit4::class)
class DetalleNivelPantallaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var comenzados = mutableListOf<Int>()
    private var volvio = 0

    private fun mostrar(estado: DetalleNivelUiState) {
        composeRule.setContent {
            ContenidoDetalleNivel(
                estado = estado,
                onVolver = { volvio++ },
                onComenzar = { comenzados += it.id },
                onReintentar = {}
            )
        }
    }

    private val primerosPasos =
        NivelDetalle(1, 1, "Primeros pasos", "lectura", "Lee palabras y oraciones cortas", "desbloqueado")

    @Test
    fun nivelDisponibleMuestraElRetoYComenzar() {
        mostrar(DetalleNivelUiState.Exito(primerosPasos))

        composeRule.onNodeWithText("Primeros pasos").assertIsDisplayed()
        composeRule.onNodeWithText("Reto de lectura").assertIsDisplayed()
        composeRule.onNodeWithTag("icono_reto").assertIsDisplayed()
        composeRule.onNodeWithTag("boton_comenzar").performClick()

        assertEquals(listOf(1), comenzados)
    }

    @Test
    fun cadaTipoDeRetoTieneSuEtiqueta() {
        mostrar(DetalleNivelUiState.Exito(primerosPasos.copy(tipoReto = "oralidad")))

        composeRule.onNodeWithText("Reto de oralidad").assertIsDisplayed()
    }

    @Test
    fun nivelCompletadoInvitaARepetirlo() {
        mostrar(DetalleNivelUiState.Exito(primerosPasos.copy(estado = "completado", puntos = 10)))

        composeRule.onNodeWithText("¡Ya lo completaste! Puedes jugarlo otra vez.").assertIsDisplayed()
        composeRule.onNodeWithTag("boton_comenzar").assertIsDisplayed()
    }

    // La regla de T-243: el nivel bloqueado no se puede comenzar
    @Test
    fun nivelBloqueadoNoSeAbreNiMuestraComenzar() {
        mostrar(DetalleNivelUiState.Bloqueado(MapaViewModel.mensajeNivelBloqueado("Primeros pasos")))

        composeRule.onNodeWithTag("mensaje_nivel_bloqueado").assertIsDisplayed()
        composeRule.onNodeWithTag("boton_comenzar").assertDoesNotExist()
        composeRule.onNodeWithText("Volver al mapa").performClick()

        assertEquals(1, volvio)
        assertEquals(emptyList<Int>(), comenzados)
    }

    @Test
    fun volverAlMapaDesdeArriba() {
        mostrar(DetalleNivelUiState.Exito(primerosPasos))

        composeRule.onNodeWithTag("boton_volver_mapa").performClick()

        assertEquals(1, volvio)
    }
}