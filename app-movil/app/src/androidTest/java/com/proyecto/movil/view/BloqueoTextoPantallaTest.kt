package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.viewmodel.ConsignaUiState
import com.proyecto.movil.viewmodel.EditorViewModel
import com.proyecto.movil.viewmodel.EnvioTextoUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// T-438 (US-21): pantalla del texto ya enviado (T-436) y del texto en cola sin internet (T-437)
class BloqueoTextoPantallaTest {

    @get:Rule
    val regla = createComposeRule()

    private val consigna = ConsignaUiState.Exito(
        ConsignaNivel(2, "Palabras nuevas", "Escribe sobre tu animal favorito.", null, "animal", 3)
    )
    private val texto = "Mi perro se llama Toby."

    @Test
    fun siYaLoEnvioVeSuTextoSinPoderEditarlo() {
        var alMapa = 0
        regla.setContent {
            ContenidoEditor(consigna, texto, {}, {}, {}, {},
                envio = EnvioTextoUiState.YaEnviado(texto), onVolverAlMapa = { alMapa++ })
        }

        regla.onNodeWithTag("titulo_ya_enviado").assertIsDisplayed()
        regla.onNodeWithTag("texto_ya_enviado").assertTextEquals(texto)
        regla.onNodeWithTag("campo_texto").assertDoesNotExist()
        regla.onNodeWithTag("boton_enviar_texto").assertDoesNotExist()
        regla.onNodeWithTag("boton_volver_mapa_ya_enviado").performScrollTo().performClick()

        assertEquals(1, alMapa)
    }

    @Test
    fun enColaAvisaYPermiteIntentarAhora() {
        var reintentos = 0
        regla.setContent {
            ContenidoEditor(consigna, texto, {}, {}, {}, {},
                envio = EnvioTextoUiState.EnCola, onReintentarEnvio = { reintentos++ })
        }

        regla.onNodeWithTag("aviso_en_cola_texto").assertTextEquals(EditorViewModel.MENSAJE_EN_COLA)
        regla.onNodeWithTag("boton_enviar_texto").assertDoesNotExist()
        regla.onNodeWithTag("boton_reintentar_texto").performClick()

        assertEquals(1, reintentos)
    }
}
