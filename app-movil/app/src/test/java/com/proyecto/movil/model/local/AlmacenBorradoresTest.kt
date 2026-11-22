package com.proyecto.movil.model.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// T-325 (US-18): borradores del editor guardados en la tablet
class AlmacenBorradoresTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val ahora = 1_000_000_000_000L
    private lateinit var almacen: AlmacenBorradores

    @Before
    fun preparar() {
        almacen = AlmacenBorradores(carpeta.root) { ahora }
    }

    @Test
    fun `sin borrador devuelve null`() {
        assertNull(almacen.leer(1, 2))
    }

    @Test
    fun `guarda y lee el texto con tildes y saltos de linea`() {
        almacen.guardar(1, 2, "Mi perro se llama Toby.\nCome croquetas y es muy juguetón.")

        assertEquals("Mi perro se llama Toby.\nCome croquetas y es muy juguetón.", almacen.leer(1, 2))
    }

    @Test
    fun `guardar otra vez reemplaza el borrador`() {
        almacen.guardar(1, 2, "Mi perro")
        almacen.guardar(1, 2, "Mi perro Toby")

        assertEquals("Mi perro Toby", almacen.leer(1, 2))
    }

    @Test
    fun `texto vacio borra el borrador`() {
        almacen.guardar(1, 2, "Mi perro")
        almacen.guardar(1, 2, "   ")

        assertNull(almacen.leer(1, 2))
    }

    @Test
    fun `cada estudiante y cada nivel tiene su borrador`() {
        almacen.guardar(1, 2, "de Ana")
        almacen.guardar(9, 2, "de Luis")
        almacen.guardar(1, 5, "relato de Ana")

        assertEquals("de Ana", almacen.leer(1, 2))
        assertEquals("de Luis", almacen.leer(9, 2))
        assertEquals("relato de Ana", almacen.leer(1, 5))
    }

    @Test
    fun `borrar quita el borrador`() {
        almacen.guardar(1, 2, "Mi perro")
        almacen.borrar(1, 2)

        assertNull(almacen.leer(1, 2))
    }

    @Test
    fun `no deja archivos temporales`() {
        almacen.guardar(1, 2, "Mi perro")

        val archivos = File(carpeta.root, "est_1").list()!!.toList()
        assertEquals(listOf("nivel_2.txt"), archivos)
    }

    @Test
    fun `limpiar borra los de mas de 30 dias y deja los recientes`() {
        almacen.guardar(1, 2, "viejo")
        File(carpeta.root, "est_1/nivel_2.txt").setLastModified(ahora - 31L * 24 * 60 * 60 * 1000)
        almacen.guardar(1, 5, "reciente")
        File(carpeta.root, "est_1/nivel_5.txt").setLastModified(ahora - 2L * 24 * 60 * 60 * 1000)

        almacen.limpiar()

        assertNull(almacen.leer(1, 2))
        assertEquals("reciente", almacen.leer(1, 5))
    }

    @Test
    fun `limpiar quita la carpeta del estudiante si queda vacia`() {
        almacen.guardar(1, 2, "viejo")
        File(carpeta.root, "est_1/nivel_2.txt").setLastModified(ahora - 40L * 24 * 60 * 60 * 1000)

        almacen.limpiar()

        assertFalse(File(carpeta.root, "est_1").exists())
    }
}