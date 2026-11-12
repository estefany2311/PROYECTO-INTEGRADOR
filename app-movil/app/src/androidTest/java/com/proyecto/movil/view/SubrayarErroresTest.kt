package com.proyecto.movil.view

import androidx.compose.ui.text.AnnotatedString
import com.proyecto.movil.model.ErrorOrtografico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// T-331 (US-19): qué partes del texto se marcan en rojo
class SubrayarErroresTest {

    private val texto = "Mi perro es muy jugeton y le gusta corer."

    private fun tramosMarcados(marcado: AnnotatedString) =
        marcado.spanStyles.map { marcado.text.substring(it.start, it.end) }

    @Test
    fun marcaCadaPalabraConError() {
        val marcado = marcarErrores(texto, listOf(
            ErrorOrtografico("jugeton", 16, 23, "ortografia", null),
            ErrorOrtografico("corer", 35, 40, "ortografia", null)
        ))

        assertEquals(listOf("jugeton", "corer"), tramosMarcados(marcado))
        assertEquals(ESTILO_ERROR, marcado.spanStyles[0].item)
    }

    @Test
    fun elTextoNoCambia() {
        val marcado = marcarErrores(texto, listOf(ErrorOrtografico("jugeton", 16, 23, "ortografia", null)))

        assertEquals(texto, marcado.text)
    }

    @Test
    fun sinErroresNoMarcaNada() {
        assertTrue(marcarErrores(texto, emptyList()).spanStyles.isEmpty())
    }

    @Test
    fun noMarcaSiLaPalabraYaNoEstaEnEseLugar() {
        val viejo = ErrorOrtografico("pero", 3, 7, "ortografia", null)

        assertTrue(marcarErrores("Mi perro es muy jugeton", listOf(viejo)).spanStyles.isEmpty())
    }

    @Test
    fun noFallaSiElErrorQuedaFueraDelTexto() {
        val fuera = ErrorOrtografico("corer", 35, 40, "ortografia", null)

        assertTrue(marcarErrores("Mi perro", listOf(fuera)).spanStyles.isEmpty())
    }

    @Test
    fun laTransformacionNoMueveElCursor() {
        val transformado = SubrayarErrores(listOf(ErrorOrtografico("jugeton", 16, 23, "ortografia", null)))
            .filter(AnnotatedString(texto))

        assertEquals(20, transformado.offsetMapping.originalToTransformed(20))
        assertEquals(texto, transformado.text.text)
    }

    @Test
    fun dosTransformacionesConLosMismosErroresSonIguales() {
        val errores = listOf(ErrorOrtografico("corer", 35, 40, "ortografia", null))

        assertEquals(SubrayarErrores(errores), SubrayarErrores(errores.toList()))
    }
}