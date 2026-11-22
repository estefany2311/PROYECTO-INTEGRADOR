package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

// T-318 (US-11): pruebas de la pantalla de grabación y del permiso de micrófono (se corren en la tablet)
class PantallaGrabacionTest {

    @get:Rule
    val compose = createComposeRule()

    private val lectura = LecturaNivel(1, 3, "El cóndor y el zorro", "Había una vez un cóndor.", 2, "basica", false)

    private var clics = mutableMapOf<String, Int>()
    private fun contar(nombre: String): () -> Unit = { clics[nombre] = (clics[nombre] ?: 0) + 1 }

    private fun mostrarPermiso(estado: EstadoPermiso) = compose.setContent {
        AppMovilTheme(dynamicColor = false) {
            ExplicacionMicrofono(
                estado,
                onPermitir = contar("permitir"),
                onAhoraNo = contar("ahoraNo"),
                onAbrirAjustes = contar("ajustes"),
                onYaLoActive = contar("yaActive")
            )
        }
    }

    private fun mostrarGrabacion(estado: GrabacionUiState) = compose.setContent {
        AppMovilTheme(dynamicColor = false) {
            ContenidoGrabacion(
                lectura, estado,
                onVolver = contar("volver"),
                onGrabar = contar("grabar"),
                onTerminar = contar("terminar"),
                onGrabarOtraVez = contar("otraVez")
            )
        }
    }

    // ---------- Permiso del micrófono ----------

    @Test
    fun primeraVez_explicaParaQueSeUsaYPidePermiso() {
        mostrarPermiso(EstadoPermiso.SIN_PEDIR)

        compose.onNodeWithText("Para escuchar cómo lees", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("boton_permitir").performClick()
        assertEquals(1, clics["permitir"])
    }

    @Test
    fun permisoDenegado_dejaIntentarOtraVez() {
        mostrarPermiso(EstadoPermiso.DENEGADO)

        compose.onNodeWithText("Sin el micrófono no puedo escucharte").assertIsDisplayed()
        compose.onNodeWithTag("boton_permitir").assertTextContains("Intentar otra vez").performClick()
        assertEquals(1, clics["permitir"])
    }

    @Test
    fun permisoDenegadoSiempre_mandaAAjustesYNoVuelveAPreguntar() {
        mostrarPermiso(EstadoPermiso.DENEGADO_SIEMPRE)

        compose.onNodeWithTag("boton_permitir").assertDoesNotExist()
        compose.onNodeWithTag("boton_ajustes").performClick()
        compose.onNodeWithTag("boton_ya_active").performClick()
        assertEquals(1, clics["ajustes"])
        assertEquals(1, clics["yaActive"])
    }

    @Test
    fun ahoraNo_vuelveAtras() {
        mostrarPermiso(EstadoPermiso.DENEGADO)

        compose.onNodeWithTag("boton_ahora_no").performClick()
        assertEquals(1, clics["ahoraNo"])
    }

    // ---------- Grabación ----------

    @Test
    fun lista_tocarMicrofonoEmpiezaAGrabar() {
        mostrarGrabacion(GrabacionUiState.Lista)

        compose.onNodeWithText("El cóndor y el zorro").assertIsDisplayed()
        compose.onNodeWithTag("boton_empezar_grabar").performClick()
        assertEquals(1, clics["grabar"])
    }

    @Test
    fun grabando_muestraIndicadorTiempoYBarra_yNoDejaVolver() {
        mostrarGrabacion(GrabacionUiState.Grabando(12_000, 0.5f))

        compose.onNodeWithTag("indicador_grabando").assertIsDisplayed()
        compose.onNodeWithTag("tiempo_grabacion").assertTextContains("0:12", substring = true)
        compose.onNodeWithTag("barra_tiempo").assertIsDisplayed()
        compose.onNodeWithTag("boton_volver_grabacion").assertIsNotEnabled()
        compose.onNodeWithTag("aviso_tiempo_final").assertDoesNotExist()

        compose.onNodeWithTag("boton_terminar_grabar").performClick()
        assertEquals(1, clics["terminar"])
    }

    @Test
    fun ultimosSegundos_avisaCuantoQueda() {
        mostrarGrabacion(GrabacionUiState.Grabando(108_000, 0.5f))

        compose.onNodeWithTag("aviso_tiempo_final").assertTextContains("0:12", substring = true)
    }

    @Test
    fun detenidaPorTiempo_explicaQueSeTerminoYQueQuedoGuardada() {
        val maximo = GrabacionViewModel.DURACION_MAXIMA_MS
        mostrarGrabacion(GrabacionUiState.Grabada(Grabacion(File("nivel_1.wav"), maximo), porTiempo = true))

        compose.onNodeWithTag("aviso_tiempo_terminado").assertIsDisplayed()
        compose.onNodeWithTag("grabacion_terminada").assertTextContains("2:00", substring = true)
        compose.onNodeWithTag("aviso_guardada").assertIsDisplayed()
    }

    @Test
    fun grabacionDeAntes_avisaQueYaGraboYDejaGrabarOtraVez() {
        mostrarGrabacion(GrabacionUiState.Grabada(Grabacion(File("nivel_1.wav"), 45_000), deAntes = true))

        compose.onNodeWithTag("grabacion_terminada").assertTextContains("Ya grabaste esta lectura", substring = true)
        compose.onNodeWithTag("aviso_tiempo_terminado").assertDoesNotExist()
        compose.onNodeWithTag("boton_grabar_otra_vez").performClick()
        assertEquals(1, clics["otraVez"])
    }

    @Test
    fun sinAudio_muestraErrorYDejaIntentarOtraVez() {
        mostrarGrabacion(GrabacionUiState.Error(GrabacionViewModel.MENSAJE_SIN_AUDIO))

        compose.onNodeWithTag("error_grabacion").assertTextContains("No escuchamos nada", substring = true)
        compose.onNodeWithText("Intentar otra vez").performClick()
        assertEquals(1, clics["otraVez"])
    }
}