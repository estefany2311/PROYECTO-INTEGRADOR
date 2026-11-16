package com.proyecto.movil.model

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

// T-209: el modelo de la app lee bien la respuesta del endpoint del mapa (T-197)
class MapaJsonTest {

    // Mismo formato que devuelve GET /api/estudiante/mapa del backend docente
    private val json = """
        {
          "estudianteId": 1, "nombres": "Ana", "apellidos": "Quispe",
          "totalNiveles": 3, "completados": 1, "puntos": 10,
          "niveles": [
            {"id": 1, "orden": 1, "nombre": "Primeros pasos", "tipoReto": "lectura",
             "descripcion": "Lee palabras y oraciones cortas", "estado": "completado", "puntos": 10},
            {"id": 2, "orden": 2, "nombre": "Palabras nuevas", "tipoReto": "escritura",
             "descripcion": "Escribe palabras y frases sencillas", "estado": "desbloqueado", "puntos": 0},
            {"id": 3, "orden": 3, "nombre": "Cuento corto", "tipoReto": "oralidad",
             "descripcion": null, "estado": "bloqueado", "puntos": 0}
          ]
        }
    """.trimIndent()

    private val mapa = Gson().fromJson(json, MapaRespuesta::class.java)

    @Test
    fun `lee el resumen del mapa`() {
        assertEquals(1L, mapa.estudianteId)
        assertEquals("Ana", mapa.nombres)
        assertEquals(3, mapa.totalNiveles)
        assertEquals(1, mapa.completados)
        assertEquals(10, mapa.puntos)
    }

    @Test
    fun `convierte el tipo de reto y el estado de cada nivel`() {
        assertEquals(TipoReto.LECTURA, mapa.niveles[0].reto)
        assertEquals(EstadoNivel.COMPLETADO, mapa.niveles[0].estadoNivel)
        assertEquals(TipoReto.ESCRITURA, mapa.niveles[1].reto)
        assertEquals(EstadoNivel.DESBLOQUEADO, mapa.niveles[1].estadoNivel)
        assertTrue(mapa.niveles[2].bloqueado)
        assertNull(mapa.niveles[2].descripcion)
    }

    @Test
    fun `un valor desconocido no rompe la app y el nivel queda bloqueado`() {
        assertEquals(TipoReto.DESCONOCIDO, TipoReto.desde("dibujo"))
        assertEquals(EstadoNivel.BLOQUEADO, EstadoNivel.desde("pausado"))
        assertEquals(EstadoNivel.BLOQUEADO, EstadoNivel.desde(null))
    }
}