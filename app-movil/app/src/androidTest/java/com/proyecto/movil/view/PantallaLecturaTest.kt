package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proyecto.movil.model.LecturaNivel
import com.proyecto.movil.viewmodel.LecturaUiState
import com.proyecto.movil.viewmodel.LecturaViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-312 (US-10): la pantalla de lectura (T-219, T-311).
 * Se prueba ContenidoLectura directamente, sin ViewModel ni backend.
 */
@RunWith(AndroidJUnit4::class)
class PantallaLecturaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val corta = LecturaNivel(1, 3, "El cóndor y el zorro", "Había una vez un cóndor.", 2, "basica", false)
    private val larga = corta.copy(texto = List(40) { "El cóndor volaba alto sobre la montaña." }.joinToString(" "))

    private val grabados = mutableListOf<Long>()
    private var achicados = 0
    private var agrandados = 0
    private var reintentos = 0
    private var volvio = 0

    private fun mostrar(
        estado: LecturaUiState,
        tamano: Int = 32,
        puedeAchicar: Boolean = true,
        puedeAgrandar: Boolean = true
    ) {
        composeRule.setContent {
            ContenidoLectura(
                estado = estado,
                onVolver = { volvio++ },
                onGrabar = { grabados += it.lecturaId },
                onReintentar = { reintentos++ },
                tamanoLetra = tamano,
                puedeAchicar = puedeAchicar,
                puedeAgrandar = puedeAgrandar,
                onAchicar = { achicados++ },
                onAgrandar = { agrandados++ }
            )
        }
    }

    @Test
    fun muestraElTituloElTextoYElBotonDeGrabar() {
        mostrar(LecturaUiState.Exito(corta))

        composeRule.onNodeWithText("Lee el texto en voz alta").assertIsDisplayed()
        composeRule.onNodeWithText("El cóndor y el zorro").assertIsDisplayed()
        composeRule.onNodeWithText("Había una vez un cóndor.").assertIsDisplayed()
        composeRule.onNodeWithTag("boton_grabar").performClick()

        assertEquals(listOf(3L), grabados)
    }

    @Test
    fun losBotonesDeLetraAvisanAlViewModel() {
        mostrar(LecturaUiState.Exito(corta))

        composeRule.onNodeWithTag("boton_letra_mas").performClick()
        composeRule.onNodeWithTag("boton_letra_menos").performClick()

        assertEquals(1, agrandados)
        assertEquals(1, achicados)
    }

    // En el tamaño máximo ya no se puede agrandar (y en el mínimo, achicar)
    @Test
    fun enLosLimitesElBotonSeDesactiva() {
        mostrar(LecturaUiState.Exito(corta), tamano = 44, puedeAgrandar = false)

        composeRule.onNodeWithTag("boton_letra_mas").assertIsNotEnabled()
        composeRule.onNodeWithTag("boton_letra_menos").assertIsEnabled()
    }

    // Texto largo: aparece el aviso y el botón de grabar sigue a la vista
    @Test
    fun conTextoLargoSeAvisaQueSigueYElBotonQuedaVisible() {
        mostrar(LecturaUiState.Exito(larga), tamano = 44)

        composeRule.onNodeWithTag("aviso_sigue_leyendo").assertIsDisplayed()
        composeRule.onNodeWithTag("boton_grabar").assertIsDisplayed()
    }

    @Test
    fun conTextoCortoNoHayAviso() {
        mostrar(LecturaUiState.Exito(corta), tamano = 24)

        composeRule.onNodeWithTag("aviso_sigue_leyendo").assertDoesNotExist()
    }

    @Test
    fun sinConexionSePuedeReintentar() {
        mostrar(LecturaUiState.Error(LecturaViewModel.MENSAJE_SIN_CONEXION, puedeReintentar = true))

        composeRule.onNodeWithText(LecturaViewModel.MENSAJE_SIN_CONEXION).assertIsDisplayed()
        composeRule.onNodeWithTag("boton_reintentar_lectura").performClick()

        assertEquals(1, reintentos)
    }

    // Sin lectura no sirve reintentar: se ofrece volver y no aparecen ni el texto ni los botones de letra
    @Test
    fun sinLecturaSoloSeOfreceVolver() {
        mostrar(LecturaUiState.Error(LecturaViewModel.MENSAJE_SIN_LECTURA, puedeReintentar = false))

        composeRule.onNodeWithTag("boton_reintentar_lectura").assertDoesNotExist()
        composeRule.onNodeWithTag("boton_grabar").assertDoesNotExist()
        composeRule.onNodeWithTag("boton_letra_mas").assertDoesNotExist()
        composeRule.onNodeWithText("Volver").performClick()

        assertEquals(1, volvio)
    }
}