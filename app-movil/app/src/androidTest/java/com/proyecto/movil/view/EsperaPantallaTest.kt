package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.viewmodel.EsperaUiState
import com.proyecto.movil.viewmodel.EsperaViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// T-418 (US-13): pantalla de espera mientras la IA escucha la lectura (se corren en la tablet)
class EsperaPantallaTest {

    @get:Rule
    val compose = createComposeRule()

    private var vueltas = 0

    private fun mostrar(estado: EsperaUiState) = compose.setContent {
        AppMovilTheme(dynamicColor = false) { ContenidoEspera(estado) { vueltas++ } }
    }

    @Test
    fun escuchando_muestraElIndicadorYNoDejaVolver() {
        mostrar(EsperaUiState.Escuchando)

        compose.onNodeWithTag("titulo_escuchando").assertIsDisplayed()
        compose.onNodeWithTag("indicador_escuchando").assertIsDisplayed()
        compose.onNodeWithTag("boton_volver_mapa_espera").assertDoesNotExist()
    }

    @Test
    fun lista_muestraLoQueEscuchoYVuelveAlMapa() {
        mostrar(EsperaUiState.Lista("Había una vez un cóndor"))

        compose.onNodeWithTag("transcripcion").assertTextContains("Había una vez un cóndor")
        compose.onNodeWithTag("boton_volver_mapa_espera").performClick()
        assertEquals(1, vueltas)
    }

    @Test
    fun listaSinPalabras_loDiceConCalma() {
        mostrar(EsperaUiState.Lista(""))

        compose.onNodeWithTag("transcripcion").assertTextContains("No escuchamos palabras", substring = true)
    }

    @Test
    fun error_muestraElMensaje() {
        mostrar(EsperaUiState.Error(EsperaViewModel.MENSAJE_ERROR_IA))

        compose.onNodeWithTag("aviso_espera").assertTextContains("profesor", substring = true)
        compose.onNodeWithTag("boton_volver_mapa_espera").assertIsDisplayed()
    }

    @Test
    fun tardaMucho_tranquilizaAlNino() {
        mostrar(EsperaUiState.TardaMucho)

        compose.onNodeWithTag("aviso_espera").assertTextContains("tardando", substring = true)
    }
}
