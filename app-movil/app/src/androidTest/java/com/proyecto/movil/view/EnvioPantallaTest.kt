package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.proyecto.movil.model.LecturaNivel
import com.proyecto.movil.model.audio.Grabacion
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.viewmodel.GrabacionUiState
import com.proyecto.movil.viewmodel.GrabacionViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

// T-417 (US-13): pantalla mientras sube la grabación, al terminar y si falla (se corren en la tablet)
class EnvioPantallaTest {

    @get:Rule
    val compose = createComposeRule()

    private val lectura = LecturaNivel(1, 5, "El cóndor y el zorro", "Había una vez un cóndor.", 2, "basica", false)
    private val grabacion = Grabacion(File("nivel_1.wav"), 45_000)
    private val clics = mutableMapOf<String, Int>()
    private fun contar(nombre: String): () -> Unit = { clics[nombre] = (clics[nombre] ?: 0) + 1 }

    private fun mostrar(estado: GrabacionUiState) = compose.setContent {
        AppMovilTheme(dynamicColor = false) {
            ContenidoGrabacion(
                lectura, estado,
                onVolver = contar("volver"),
                onGrabar = contar("grabar"),
                onTerminar = contar("terminar"),
                onGrabarOtraVez = contar("otraVez"),
                onEnviar = contar("enviar")
            )
        }
    }

    @Test
    fun enviando_muestraElPorcentajeYLaBarra() {
        mostrar(GrabacionUiState.Enviando(grabacion, 0.45f))

        compose.onNodeWithTag("texto_enviando").assertTextContains("45 %", substring = true)
        compose.onNodeWithTag("barra_envio").assertIsDisplayed()
    }

    @Test
    fun enviando_noSePuedeVolverNiEnviarOtraVez() {
        mostrar(GrabacionUiState.Enviando(grabacion, 0.2f))

        compose.onNodeWithTag("boton_enviar_grabacion").assertDoesNotExist()
        compose.onNodeWithTag("boton_volver_grabacion").performClick()
        assertEquals(null, clics["volver"])
    }

    @Test
    fun enviada_ofreceVolverAlMapa() {
        mostrar(GrabacionUiState.Enviada(50L))

        compose.onNodeWithTag("lectura_enviada").assertIsDisplayed()
        compose.onNodeWithTag("boton_volver_mapa").performClick()
        assertEquals(1, clics["volver"])
    }

    @Test
    fun sinInternet_permiteReintentar() {
        mostrar(GrabacionUiState.ErrorEnvio(grabacion, GrabacionViewModel.MENSAJE_SIN_CONEXION, puedeReintentar = true))

        compose.onNodeWithTag("error_envio").assertTextContains("internet", substring = true)
        compose.onNodeWithTag("boton_reintentar_envio").performClick()
        assertEquals(1, clics["enviar"])
    }

    @Test
    fun rechazada_soloOfreceVolverAGrabar() {
        mostrar(GrabacionUiState.ErrorEnvio(grabacion, "La grabación es muy corta. Intenta leer otra vez", puedeReintentar = false))

        compose.onNodeWithTag("boton_reintentar_envio").assertDoesNotExist()
        compose.onNodeWithTag("boton_grabar_otra_vez").performClick()
        assertEquals(1, clics["otraVez"])
    }

    // T-419
    @Test
    fun enCola_avisaQueSeEnviaraSolaYPermiteIntentarAhora() {
        mostrar(GrabacionUiState.EnCola(grabacion))

        compose.onNodeWithTag("aviso_en_cola").assertTextContains("se enviará sola", substring = true)
        compose.onNodeWithTag("boton_enviar_grabacion").assertDoesNotExist()
        compose.onNodeWithTag("boton_enviar_ahora").performClick()
        assertEquals(1, clics["enviar"])
        compose.onNodeWithTag("boton_volver_en_cola").performClick()
        assertEquals(1, clics["volver"])
    }
}
