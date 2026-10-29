package com.proyecto.movil.model

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * T-213 (US-06): el estado de cada nivel que manda el backend en la respuesta del mapa.
 * La app decide con él qué niveles puede abrir el niño (T-239 y T-240 lo usan).
 */
class EstadoNivelTest {

    private fun nivel(estado: String?) = NivelMapa(1, 1, "Primeros pasos", "lectura", null, estado ?: "")

    @Test
    fun `lee los tres estados del backend`() {
        assertEquals(EstadoNivel.BLOQUEADO, EstadoNivel.desde("bloqueado"))
        assertEquals(EstadoNivel.DESBLOQUEADO, EstadoNivel.desde("desbloqueado"))
        assertEquals(EstadoNivel.COMPLETADO, EstadoNivel.desde("completado"))
    }

    @Test
    fun `no importan las mayusculas ni los espacios`() {
        assertEquals(EstadoNivel.COMPLETADO, EstadoNivel.desde("COMPLETADO"))
        assertEquals(EstadoNivel.DESBLOQUEADO, EstadoNivel.desde(" Desbloqueado "))
    }

    // Seguridad para el niño: ante la duda, el nivel queda cerrado
    @Test
    fun `un estado desconocido vacio o nulo cuenta como bloqueado`() {
        assertEquals(EstadoNivel.BLOQUEADO, EstadoNivel.desde("en_revision"))
        assertEquals(EstadoNivel.BLOQUEADO, EstadoNivel.desde(""))
        assertEquals(EstadoNivel.BLOQUEADO, EstadoNivel.desde(null))
    }

    @Test
    fun `un nivel bloqueado no esta disponible`() {
        val n = nivel("bloqueado")
        assertTrue(n.bloqueado)
        assertFalse(n.disponible)
        assertFalse(n.completado)
    }

    @Test
    fun `un nivel desbloqueado esta disponible y no completado`() {
        val n = nivel("desbloqueado")
        assertFalse(n.bloqueado)
        assertTrue(n.disponible)
        assertFalse(n.completado)
    }

    // Un nivel completado se puede volver a jugar
    @Test
    fun `un nivel completado sigue disponible`() {
        val n = nivel("completado")
        assertTrue(n.completado)
        assertTrue(n.disponible)
        assertFalse(n.bloqueado)
    }

    // Si el JSON no trae "estado", Gson deja el campo vacío: la app no se cae y el nivel queda cerrado
    @Test
    fun `si la respuesta no trae estado el nivel queda bloqueado`() {
        val json = """{"id": 4, "orden": 4, "nombre": "Sin estado", "tipoReto": "lectura", "puntos": 0}"""
        val n = Gson().fromJson(json, NivelMapa::class.java)

        assertEquals(EstadoNivel.BLOQUEADO, n.estadoNivel)
        assertFalse(n.disponible)
    }
}