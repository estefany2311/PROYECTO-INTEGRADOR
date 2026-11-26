package com.proyecto.movil.view

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.view.componentes.BarraAventura
import com.proyecto.movil.view.componentes.MenuInferior
import com.proyecto.movil.view.componentes.Pestana
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// Barra de contadores, menú inferior y las pestañas Logros y Perfil
class MenuAventuraTest {

    @get:Rule
    val regla = createComposeRule()

    @Test
    fun laBarraMuestraEstrellasYPuntosYSinRachaNoLaMuestra() {
        regla.setContent { AppMovilTheme { BarraAventura("Ana", "", racha = null, estrellas = 12, puntos = 86) } }

        regla.onNodeWithTag("barra_estrellas").assertContentDescriptionEquals("12 estrellas")
        regla.onNodeWithTag("barra_puntos").assertContentDescriptionEquals("86 puntos")
        regla.onNodeWithTag("barra_racha").assertDoesNotExist()
    }

    @Test
    fun conRachaLaMuestra() {
        regla.setContent { AppMovilTheme { BarraAventura("Ana", "", racha = 4, estrellas = 0, puntos = 0) } }

        regla.onNodeWithTag("barra_racha").assertContentDescriptionEquals("Racha de 4 días")
    }

    @Test
    fun elMenuMarcaLaPestanaActualYAvisaCualSeEligio() {
        val elegidas = mutableListOf<Pestana>()
        regla.setContent { AppMovilTheme { MenuInferior(actual = Pestana.AVENTURA, onElegir = { elegidas += it }) } }

        regla.onNodeWithTag("pestana_aventura").assertIsSelected()
        regla.onNodeWithTag("pestana_logros").performClick()

        assertEquals(listOf(Pestana.LOGROS), elegidas)
    }

    @Test
    fun logrosMuestraElAvancePorTipoDeReto() {
        val mapa = MapaRespuesta(
            7L, "Ana", "Pérez", 3, 1, 18,
            listOf(
                NivelMapa(1, 1, "Uno", "lectura", null, "completado"),
                NivelMapa(2, 2, "Dos", "lectura", null, "desbloqueado"),
                NivelMapa(3, 3, "Tres", "escritura", null, "bloqueado")
            ),
            estrellas = 3
        )
        regla.setContent { AppMovilTheme { ContenidoLogros(mapa) } }

        regla.onNodeWithText("1 de 2").assertExists()     // lectura
        regla.onNodeWithText("0 de 1").assertExists()     // escritura
        regla.onNodeWithText("Oralidad").assertDoesNotExist()   // no hay niveles de oralidad
    }

    @Test
    fun enPerfilSePuedeSalir() {
        var salir = 0
        regla.setContent { AppMovilTheme { ContenidoPerfilAlumno("Ana", "", 1, 3, onSalir = { salir++ }) } }

        regla.onNodeWithTag("perfil_avance").assertExists()
        regla.onNodeWithTag("boton_salir").performClick()

        assertEquals(1, salir)
    }
}
