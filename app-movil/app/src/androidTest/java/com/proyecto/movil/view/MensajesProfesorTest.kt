package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.MensajeDocente
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.view.componentes.MensajesProfesor
import org.junit.Rule
import org.junit.Test

// Sección "Mensajes de tu profesor" de la pestaña Logros
class MensajesProfesorTest {

    @get:Rule
    val regla = createComposeRule()

    private val lectura = MensajeDocente(80L, 1, "Mi primera lectura", "audio", "Buen trabajo, sigue practicando", "2026-11-26T04:30:00")
    private val texto = MensajeDocente(81L, 2, "Mi mascota", "texto", "Cuida las mayúsculas", "2026-11-25T10:00:00")

    @Test
    fun muestraCadaComentarioConSuRetoYFecha() {
        regla.setContent { AppMovilTheme { MensajesProfesor(listOf(lectura, texto)) } }

        regla.onNodeWithTag("mensaje_80").assertIsDisplayed()
        regla.onNodeWithText("Buen trabajo, sigue practicando").assertIsDisplayed()
        regla.onNodeWithText("Lectura · Mi primera lectura").assertIsDisplayed()
        regla.onNodeWithText("26 nov").assertIsDisplayed()
        regla.onNodeWithText("Escritura · Mi mascota").assertIsDisplayed()
    }

    @Test
    fun sinComentariosExplicaQueAquiApareceran() {
        regla.setContent { AppMovilTheme { MensajesProfesor(emptyList()) } }

        regla.onNodeWithTag("sin_mensajes").assertIsDisplayed()
    }

    @Test
    fun laPestanaLogrosIncluyeLosMensajes() {
        val mapa = MapaRespuesta(7L, "Ana", "Rojas", 0, 0, 20, emptyList(), estrellas = 3)
        regla.setContent { AppMovilTheme { ContenidoLogros(mapa, listOf(lectura)) } }

        regla.onNodeWithText("Buen trabajo, sigue practicando").performScrollTo().assertIsDisplayed()
    }
}
