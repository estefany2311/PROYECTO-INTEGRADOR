package com.proyecto.movil.view

import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.model.TipoReto
import org.junit.Assert.assertEquals
import org.junit.Test

// "Mis logros": cuántos niveles de cada tipo de reto completó
class AvancePorRetoTest {

    private fun nivel(id: Int, reto: String, estado: String) = NivelMapa(id, id, "Nivel $id", reto, null, estado)

    @Test
    fun cuentaLosCompletadosDeCadaTipo() {
        val mapa = MapaRespuesta(
            7L, "Ana", "Pérez", 5, 2, 18,
            listOf(
                nivel(1, "lectura", "completado"),
                nivel(2, "escritura", "completado"),
                nivel(3, "lectura", "desbloqueado"),
                nivel(4, "oralidad", "bloqueado"),
                nivel(5, "escritura", "bloqueado")
            ),
            estrellas = 5
        )

        assertEquals(
            listOf(
                AvanceReto(TipoReto.LECTURA, 1, 2),
                AvanceReto(TipoReto.ESCRITURA, 1, 2),
                AvanceReto(TipoReto.ORALIDAD, 0, 1)
            ),
            avancePorReto(mapa)
        )
    }
}
