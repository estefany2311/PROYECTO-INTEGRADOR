package com.proyecto.movil.view

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.proyecto.movil.model.LecturaNivel
import com.proyecto.movil.model.audio.Grabacion
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.viewmodel.GrabacionUiState
import com.proyecto.movil.viewmodel.GrabacionViewModel
import com.proyecto.movil.viewmodel.Reproduccion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

// T-411 (US-12): escuchar la grabación, volver a grabar y el botón Enviar (se corren en la tablet)
class ReproductorPantallaTest {

    @get:Rule
    val compose = createComposeRule()

    private val lectura = LecturaNivel(1, 3, "El cóndor y el zorro", "Había una vez un cóndor.", 2, "basica", false)
    private val grabada = GrabacionUiState.Grabada(Grabacion(File("nivel_1.wav"), 45_000))

    private val clics = mutableMapOf<String, Int>()
    private fun contar(nombre: String): () -> Unit = { clics[nombre] = (clics[nombre] ?: 0) + 1 }

    private fun mostrar(estado: GrabacionUiState, reproduccion: Reproduccion = Reproduccion()) = compose.setContent {
        AppMovilTheme(dynamicColor = false) {
            ContenidoGrabacion(
                lectura, estado,
                onVolver = contar("volver"),
                onGrabar = contar("grabar"),
                onTerminar = contar("terminar"),
                onGrabarOtraVez = contar("otraVez"),
                reproduccion = reproduccion,
                onEscuchar = contar("escuchar"),
                onEnviar = contar("enviar")
            )
        }
    }

    // ---------- Escuchar ----------

    @Test
    fun grabada_muestraElReproductorEnCero() {
        mostrar(grabada)

        compose.onNodeWithTag("reproductor_grabacion").assertIsDisplayed()
        compose.onNodeWithTag("tiempo_reproduccion").assertTextContains("0:00 / 0:45")
        compose.onNodeWithTag("boton_escuchar").assertContentDescriptionEquals("Escuchar mi grabación")
    }

    @Test
    fun tocarEscuchar_avisaAlViewModel() {
        mostrar(grabada)

        compose.onNodeWithTag("boton_escuchar").performClick()

        assertEquals(1, clics["escuchar"])
    }

    @Test
    fun mientrasSuena_muestraPorDondeVaYElBotonPausa() {
        mostrar(grabada, Reproduccion(reproduciendo = true, posicionMs = 18_000))

        compose.onNodeWithTag("tiempo_reproduccion").assertTextContains("0:18 / 0:45")
        compose.onNodeWithTag("boton_escuchar").assertContentDescriptionEquals("Pausar mi grabación")
    }

    @Test
    fun siNoSePuedeEscuchar_muestraElAviso() {
        mostrar(grabada, Reproduccion(error = GrabacionViewModel.MENSAJE_NO_SE_ESCUCHA))

        compose.onNodeWithTag("tiempo_reproduccion").assertTextContains("No pudimos reproducir", substring = true)
    }

    @Test
    fun antesDeGrabar_noHayReproductor() {
        mostrar(GrabacionUiState.Lista)

        compose.onNodeWithTag("reproductor_grabacion").assertDoesNotExist()
    }

    // ---------- Volver a grabar ----------

    @Test
    fun volverAGrabar_avisaAlViewModel() {
        mostrar(grabada)

        compose.onNodeWithText("Volver a grabar").performClick()

        assertEquals(1, clics["otraVez"])
    }

    // ---------- Enviar ----------

    @Test
    fun sinGrabacion_enviarEstaDesactivado() {
        mostrar(GrabacionUiState.Lista)

        compose.onNodeWithTag("boton_enviar_grabacion").assertIsNotEnabled()
    }

    @Test
    fun conError_enviarEstaDesactivado() {
        mostrar(GrabacionUiState.Error(GrabacionViewModel.MENSAJE_SIN_AUDIO))

        compose.onNodeWithTag("boton_enviar_grabacion").assertIsNotEnabled()
    }

    @Test
    fun mientrasGraba_noSeVeEnviar() {
        mostrar(GrabacionUiState.Grabando(5_000, 0.3f))

        compose.onNodeWithTag("boton_enviar_grabacion").assertDoesNotExist()
    }

    @Test
    fun conGrabacion_enviarSeActivaYAvisa() {
        mostrar(grabada)

        compose.onNodeWithTag("boton_enviar_grabacion").assertIsEnabled().performClick()

        assertEquals(1, clics["enviar"])
    }
}
