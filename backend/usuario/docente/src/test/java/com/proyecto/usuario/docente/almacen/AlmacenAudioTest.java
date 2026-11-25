package com.proyecto.usuario.docente.almacen;

import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-372 (US-32): la carpeta de audios solo entrega archivos que están dentro de
 * ella; acepta una ruta relativa con la subcarpeta del alumno o la URL completa
 * que guarda el módulo estudiante.
 */
class AlmacenAudioTest {

    @TempDir
    Path carpeta;

    AlmacenAudio almacen;

    @BeforeEach
    void preparar() {
        almacen = new AlmacenAudio(carpeta);
    }

    @Test
    void tomaElNombreDeUnArchivoSimple() {
        assertEquals("evidencia-30.mp3", AlmacenAudio.nombreDe("evidencia-30.mp3"));
    }

    @Test
    void tomaElNombreDeUnaUrlCompleta() {
        assertEquals("evidencia-30.mp3",
                AlmacenAudio.nombreDe("https://cdn.ejemplo.pe/evidencias/evidencia-30.mp3"));
    }

    @Test
    void ignoraLaQueryStringDeLaUrl() {
        assertEquals("evidencia-30.mp3",
                AlmacenAudio.nombreDe("https://cdn.ejemplo.pe/audios/evidencia-30.mp3?v=3"));
    }

    @Test
    void tomaElNombreDeUnaRutaConBarrasInvertidas() {
        assertEquals("evidencia-30.m4a",
                AlmacenAudio.nombreDe("C:\\audios\\evidencia-30.m4a"));
    }

    @Test
    void resuelveUnArchivoQueEstaEnLaCarpeta() throws Exception {
        Path archivo = carpeta.resolve("evidencia-30.mp3");
        Files.write(archivo, new byte[]{1, 2, 3});

        Path resuelto = almacen.resolver("evidencia-30.mp3");

        assertEquals(archivo.toAbsolutePath().normalize(), resuelto);
        assertTrue(resuelto.startsWith(almacen.directorio()));
    }

    @Test
    void resuelveUnaRutaRelativaConLaSubcarpetaDelAlumno() throws Exception {
        Path carpetaAlumno = Files.createDirectories(carpeta.resolve("est_7"));
        Path archivo = carpetaAlumno.resolve("nivel_1_20261117-221100_ab12cd34.wav");
        Files.write(archivo, new byte[]{1, 2, 3});

        Path resuelto = almacen.resolver(
                "est_7/nivel_1_20261117-221100_ab12cd34.wav");

        assertEquals(archivo.toAbsolutePath().normalize(), resuelto);
    }

    @Test
    void resuelveUnaRutaRelativaConBarrasInvertidas() throws Exception {
        Path carpetaAlumno = Files.createDirectories(carpeta.resolve("est_7"));
        Path archivo = carpetaAlumno.resolve("voz.wav");
        Files.write(archivo, new byte[]{1});

        assertEquals(archivo.toAbsolutePath().normalize(),
                almacen.resolver("est_7\\voz.wav"));
    }

    @Test
    void resuelveUnaUrlCompletaPorElNombreDelArchivo() throws Exception {
        Path archivo = carpeta.resolve("evidencia-30.mp3");
        Files.write(archivo, new byte[]{1});

        assertEquals(archivo.toAbsolutePath().normalize(),
                almacen.resolver("https://cdn.ejemplo.pe/audios/evidencia-30.mp3?v=3"));
    }

    @Test
    void unArchivoQueNoExisteDevuelve404() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> almacen.resolver("no-existe.mp3"));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("El audio no está disponible", error.getMessage());
    }

    @Test
    void rechazaUnNombreConPuntosSuspensivos() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> almacen.resolver(".."));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
    }

    @Test
    void rechazaUnaRutaQueIntentaSalirDeLaCarpeta() throws Exception {
        Files.createDirectories(carpeta.resolve("est_7"));
        Path fuera = carpeta.getParent().resolve("fuera-t372.wav");
        Files.write(fuera, new byte[]{9});
        try {
            ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                    () -> almacen.resolver("est_7/../../fuera-t372.wav"));

            assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        } finally {
            Files.deleteIfExists(fuera);
        }
    }

    @Test
    void rechazaUnNombreVacio() {
        assertThrows(ReglaNegocioException.class, () -> almacen.resolver(""));
        assertThrows(ReglaNegocioException.class, () -> almacen.resolver(null));
    }

    @Test
    void rechazaUnaRutaAbsolutaAunqueElArchivoExistaEnLaCarpeta() throws Exception {
        Path dentro = carpeta.resolve("secreto-fuera.mp3");
        Files.write(dentro, new byte[]{1});

        // una ruta absoluta no se interpreta como relativa a la carpeta
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> almacen.resolver(dentro.toAbsolutePath().toString()));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
    }

    @Test
    void elTipoDeContenidoSaleDeLaExtension() throws Exception {
        assertEquals("audio/mpeg", tipo("voz.mp3"));
        assertEquals("audio/wav", tipo("voz.wav"));
        assertEquals("audio/mp4", tipo("voz.m4a"));
        assertEquals("audio/aac", tipo("voz.aac"));
        assertEquals("audio/ogg", tipo("voz.ogg"));
        assertEquals("audio/webm", tipo("voz.webm"));
        assertEquals("application/octet-stream", tipo("voz.raro"));
    }

    private String tipo(String nombre) throws Exception {
        Path archivo = carpeta.resolve(nombre);
        Files.write(archivo, new byte[]{1});
        return almacen.tipoContenido(archivo);
    }
}
