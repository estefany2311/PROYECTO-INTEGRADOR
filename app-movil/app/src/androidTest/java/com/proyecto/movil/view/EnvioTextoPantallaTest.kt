package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.model.ResultadoEscritura
import com.proyecto.movil.model.ResultadoReto
import com.proyecto.movil.viewmodel.ConsignaUiState
import com.proyecto.movil.viewmodel.EnvioTextoUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// T-435 (US-21): confirmación, envío y pantalla de texto enviado
class EnvioTextoPantallaTest {

    @get:Rule
    val regla = createComposeRule()

    private val consigna = ConsignaUiState.Exito(
        ConsignaNivel(2, "Palabras nuevas", "Escribe sobre tu animal favorito.", null, "animal", 3)
    )
    private val texto = "Mi perro se llama Toby."

    @Test
    fun alConfirmarSeEnvia() {
        var confirmado = 0
        var cancelado = 0
        regla.setContent {
            ContenidoEditor(consigna, texto, {}, {}, {}, {},
                envio = EnvioTextoUiState.Confirmando,
                onConfirmarEnvio = { confirmado++ },
                onCancelarEnvio = { cancelado++ })
        }

        regla.onNodeWithTag("dialogo_enviar_texto").assertIsDisplayed()
        regla.onNodeWithTag("boton_confirmar_envio").performClick()

        assertEquals(1, confirmado)
        assertEquals(0, cancelado)
    }

    @Test
    fun puedeSeguirEscribiendo() {
        var cancelado = 0
        regla.setContent {
            ContenidoEditor(consigna, texto, {}, {}, {}, {},
                envio = EnvioTextoUiState.Confirmando, onCancelarEnvio = { cancelado++ })
        }

        regla.onNodeWithTag("boton_cancelar_envio").performClick()

        assertEquals(1, cancelado)
    }

    @Test
    fun mientrasSeEnviaElBotonNoSePuedeTocar() {
        regla.setContent { ContenidoEditor(consigna, texto, {}, {}, {}, {}, envio = EnvioTextoUiState.Enviando) }

        // En la tablet la hoja no se desliza (solo en pantallas bajas): el botón ya está a la vista
        regla.onNodeWithTag("boton_enviar_texto").assertIsNotEnabled()
    }

    @Test
    fun siNoSePudoEnviarMuestraElAviso() {
        regla.setContent {
            ContenidoEditor(consigna, texto, {}, {}, {}, {}, envio = EnvioTextoUiState.Error("No hay internet."))
        }

        regla.onNodeWithTag("aviso_envio").assertTextEquals("No hay internet.")
    }

    @Test
    fun alEnviarMuestraEstrellasNivelNuevoYVolverAlMapa() {
        var alMapa = 0
        val enviado = EnvioTextoUiState.Enviado(
            ResultadoEscritura(85, 2, "¡Muy bien! Revisa las tildes."),
            ResultadoReto(2, "completado", 17, 2, true, 1, 3, "Lee y cuenta", 17, true)
        )
        regla.setContent { ContenidoEditor(consigna, texto, {}, {}, {}, {}, envio = enviado, onVolverAlMapa = { alMapa++ }) }

        regla.onNodeWithTag("titulo_texto_enviado").assertIsDisplayed()
        regla.onNodeWithTag("estrellas_texto").assertIsDisplayed()
        regla.onNodeWithTag("nivel_desbloqueado_texto").performScrollTo().assertIsDisplayed()
        regla.onNodeWithTag("campo_texto").assertDoesNotExist()
        regla.onNodeWithTag("boton_volver_mapa_texto").performScrollTo().performClick()

        assertEquals(1, alMapa)
    }
}