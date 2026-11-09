package com.proyecto.movil.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.proyecto.movil.R
import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.viewmodel.ConsignaUiState
import com.proyecto.movil.viewmodel.ConsignaViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// T-322 (US-17): pruebas de la pantalla de consigna (T-321). Se corren en la tablet o el emulador.
class PantallaConsignaTest {

    @get:Rule
    val compose = createComposeRule()

    private val animal = ConsignaNivel(
        2, "Palabras nuevas", "Escribe tres oraciones sobre tu animal favorito.",
        "Cuenta cómo es, qué come y dónde vive.", "animal", 15
    )

    private val clics = mutableMapOf<String, Int>()
    private var empezoCon: ConsignaNivel? = null

    private fun mostrar(estado: ConsignaUiState) = compose.setContent {
        AppMovilTheme(dynamicColor = false) {
            ContenidoConsigna(
                estado,
                onVolver = { clics["volver"] = (clics["volver"] ?: 0) + 1 },
                onEmpezar = { empezoCon = it },
                onReintentar = { clics["reintentar"] = (clics["reintentar"] ?: 0) + 1 }
            )
        }
    }

    @Test
    fun muestraNombreDelNivelConsignaPistaYMinimoDePalabras() {
        mostrar(ConsignaUiState.Exito(animal))

        compose.onNodeWithTag("titulo_consigna").assertTextContains("Palabras nuevas")
        compose.onNodeWithText("Tu reto").assertIsDisplayed()
        compose.onNodeWithTag("texto_consigna").assertTextContains("Escribe tres oraciones sobre tu animal favorito.")
        compose.onNodeWithTag("pista_consigna").assertTextContains("Cuenta cómo es", substring = true)
        compose.onNodeWithTag("palabras_minimas").assertTextContains("Escribe al menos 15 palabras")
    }

    @Test
    fun laIlustracionCorrespondeAlTema() {
        mostrar(ConsignaUiState.Exito(animal))

        compose.onNodeWithContentDescription("Ilustración: huella de un animal").assertIsDisplayed()
    }

    @Test
    fun sinPistaNoMuestraElRecuadro() {
        mostrar(ConsignaUiState.Exito(animal.copy(pista = null)))

        compose.onNodeWithTag("pista_consigna").assertDoesNotExist()
        compose.onNodeWithTag("texto_consigna").assertIsDisplayed()
    }

    @Test
    fun empezarAEscribirEntregaLaConsigna() {
        mostrar(ConsignaUiState.Exito(animal))

        compose.onNodeWithTag("boton_empezar_escribir").performScrollTo().performClick()

        assertEquals(animal, empezoCon)
    }

    @Test
    fun volverRegresaAlDetalle() {
        mostrar(ConsignaUiState.Exito(animal))

        compose.onNodeWithTag("boton_volver_consigna").performClick()

        assertEquals(1, clics["volver"])
    }

    @Test
    fun mientrasCargaSeVeElIndicador() {
        mostrar(ConsignaUiState.Cargando)

        compose.onNodeWithTag("cargando_consigna").assertIsDisplayed()
        compose.onNodeWithTag("boton_empezar_escribir").assertDoesNotExist()
    }

    @Test
    fun sinConexionDejaReintentar() {
        mostrar(ConsignaUiState.Error(ConsignaViewModel.MENSAJE_SIN_CONEXION, puedeReintentar = true))

        compose.onNodeWithTag("error_consigna").assertTextContains(ConsignaViewModel.MENSAJE_SIN_CONEXION)
        compose.onNodeWithTag("boton_reintentar_consigna").performClick()

        assertEquals(1, clics["reintentar"])
    }

    @Test
    fun sinConsignaSoloDejaVolver() {
        mostrar(ConsignaUiState.Error(ConsignaViewModel.MENSAJE_SIN_CONSIGNA, puedeReintentar = false))

        compose.onNodeWithTag("boton_reintentar_consigna").assertDoesNotExist()
        compose.onNodeWithTag("boton_volver_error_consigna").performClick()

        assertEquals(1, clics["volver"])
    }

    // Si el backend manda un dibujo que la app no conoce, se usa el lápiz en vez de fallar
    @Test
    fun ilustracionDesconocidaUsaElLapiz() {
        assertEquals(R.drawable.ic_reto_escritura, iconoIlustracion("dinosaurio"))
        assertEquals(R.drawable.ic_ilustracion_animal, iconoIlustracion("ANIMAL"))
        assertEquals(R.drawable.ic_ilustracion_libro, iconoIlustracion("libro"))
    }
}