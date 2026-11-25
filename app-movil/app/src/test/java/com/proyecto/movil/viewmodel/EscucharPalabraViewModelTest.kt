package com.proyecto.movil.viewmodel

import com.proyecto.movil.model.ResultadoEstado
import com.proyecto.movil.model.audio.IPronunciador
import com.proyecto.movil.model.repository.IEvidenciaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// T-430 (US-14): al tocar una palabra por mejorar se dice en voz alta y queda elegida
class EscucharPalabraViewModelTest {

    private class PronunciadorFalso : IPronunciador {
        val dichas = mutableListOf<String>()
        override fun decir(palabra: String) {
            dichas += palabra
        }
    }

    private val evidencias = object : IEvidenciaRepository {
        override suspend fun estado(evidenciaId: Long) = ResultadoEstado.NoEncontrada
    }

    private val pronunciador = PronunciadorFalso()
    private val viewModel = EsperaViewModel(evidencias) { pronunciador }

    @Test
    fun diceLaPalabraSinPuntuacion() {
        viewModel.escuchar("cóndor.")

        assertEquals(listOf("cóndor"), pronunciador.dichas)
        assertEquals("cóndor", viewModel.palabraElegida.value)
    }

    @Test
    fun cadaToqueLaVuelveADecir() {
        viewModel.escuchar("vez")
        viewModel.escuchar("vez")

        assertEquals(listOf("vez", "vez"), pronunciador.dichas)
    }

    @Test
    fun unSignoSoloNoSeDice() {
        viewModel.escuchar("—")

        assertTrue(pronunciador.dichas.isEmpty())
        assertNull(viewModel.palabraElegida.value)
    }
}
