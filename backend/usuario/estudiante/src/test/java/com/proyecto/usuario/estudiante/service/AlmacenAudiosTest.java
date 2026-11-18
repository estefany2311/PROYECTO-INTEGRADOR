package com.proyecto.usuario.estudiante.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

// T-414 (US-13): los audios quedan en una carpeta por alumno, con nombre único
class AlmacenAudiosTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-17T15:15:30Z"), ZoneId.of("America/Lima"));

    @TempDir Path carpeta;

    private AlmacenAudios almacen() {
        return new AlmacenAudios(carpeta.toString(), RELOJ);
    }

    @Test
    void guardaEnLaCarpetaDelAlumnoConNivelYFecha() throws Exception {
        byte[] audio = {1, 2, 3};

        String clave = almacen().guardar(7L, 3, audio);

        assertTrue(clave.startsWith("est_7/nivel_3_20261117-101530_"), clave);
        assertTrue(clave.endsWith(".wav"));
        assertArrayEquals(audio, Files.readAllBytes(carpeta.resolve(clave)));
    }

    @Test
    void dosGrabacionesDelMismoNivelNoSePisan() {
        AlmacenAudios almacen = almacen();

        assertNotEquals(almacen.guardar(7L, 3, new byte[]{1}), almacen.guardar(7L, 3, new byte[]{2}));
    }

    @Test
    void borrarQuitaElArchivo() {
        AlmacenAudios almacen = almacen();
        String clave = almacen.guardar(7L, 3, new byte[]{1});

        almacen.borrar(clave);

        assertFalse(Files.exists(carpeta.resolve(clave)));
    }

    @Test
    void noSePuedeSalirDeLaCarpetaDeAudios() {
        assertThrows(IllegalArgumentException.class, () -> almacen().ruta("../otro/archivo.wav"));
    }
}
