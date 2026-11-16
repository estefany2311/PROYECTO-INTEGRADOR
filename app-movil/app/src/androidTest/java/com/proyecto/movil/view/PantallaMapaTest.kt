package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.viewmodel.MapaUiState
import com.proyecto.movil.viewmodel.MapaViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PantallaMapaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tipos = listOf("lectura", "escritura", "oralidad")
    private val mapa = MapaRespuesta(
        estudianteId = 1, nombres = "Ana", apellidos = "Quispe",
        totalNiveles = 6, completados = 1, puntos = 10, estrellas = 6,
        niveles = (1..6).map { n ->
            NivelMapa(n, n, "Nivel $n", tipos[(n - 1) % 3], null, if (n == 1) "completado" else "bloqueado")
        }
    )

    private var reintentos = 0

    private fun mostrar(estado: MapaUiState) {
        composeRule.setContent { ContenidoMapa(estado = estado, onReintentar = { reintentos++ }) }
    }

    @Test
    fun mientrasCargaSeVeLaRuedita() {
        mostrar(MapaUiState.Cargando)

        composeRule.onNodeWithTag("cargando_mapa").assertIsDisplayed()
    }

    @Test
    fun conErrorSeVeElMensajeYSePuedeReintentar() {
        mostrar(MapaUiState.Error(MapaViewModel.MENSAJE_ERROR))

        composeRule.onNodeWithText(MapaViewModel.MENSAJE_ERROR).assertIsDisplayed()
        composeRule.onNodeWithTag("boton_reintentar").performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun elResumenMuestraElAvanceYLosPuntos() {
        mostrar(MapaUiState.Exito(mapa))

        composeRule.onNodeWithText("Completaste 1 de 6 niveles").assertIsDisplayed()
        composeRule.onNodeWithText("10 puntos").assertIsDisplayed()
    }

    // T-304 (US-09): la cabecera muestra estrellas y puntos por separado
    @Test
    fun laCabeceraMuestraEstrellasYPuntos() {
        mostrar(MapaUiState.Exito(mapa))

        composeRule.onNodeWithTag("contador_estrellas").assertTextEquals("6 estrellas")
        composeRule.onNodeWithTag("contador_puntos").assertTextEquals("10 puntos")
    }

    @Test
    fun conUnaSolaEstrellaSeEscribeEnSingular() {
        mostrar(MapaUiState.Exito(mapa.copy(estrellas = 1, puntos = 1)))

        composeRule.onNodeWithTag("contador_estrellas").assertTextEquals("1 estrella")
        composeRule.onNodeWithTag("contador_puntos").assertTextEquals("1 punto")
    }

    @Test
    fun alumnoSinEstrellasTodaviaVeCero() {
        mostrar(MapaUiState.Exito(mapa.copy(estrellas = 0)))

        composeRule.onNodeWithTag("contador_estrellas").assertTextEquals("0 estrellas")
    }

    // Los 6 niveles están en el recorrido (los de abajo se ven al deslizar)
    @Test
    fun seDibujanLosSeisNivelesEnOrden() {
        mostrar(MapaUiState.Exito(mapa))

        (1..6).forEach { n -> composeRule.onNodeWithTag("nivel_$n").assertExists() }
        composeRule.onNodeWithTag("nivel_1").assertIsDisplayed()
    }

    // TalkBack lee el número y el nombre de cada nivel
    @Test
    fun cadaNivelTieneDescripcionParaLectorDePantalla() {
        mostrar(MapaUiState.Exito(mapa))

        composeRule.onNodeWithContentDescription("Nivel 1: Nivel 1").assertExists()
        composeRule.onNodeWithContentDescription("Nivel 6: Nivel 6").assertExists()
    }
}