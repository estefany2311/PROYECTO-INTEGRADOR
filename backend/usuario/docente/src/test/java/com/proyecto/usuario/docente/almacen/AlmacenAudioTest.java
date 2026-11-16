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
 * ella y acepta tanto un nombre simple como la URL completa que guarda el
 * módulo estudiante.
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
    void rechazaUnNombreVacio() {
        assertThrows(ReglaNegocioException.class, () -> almacen.resolver(""));
        assertThrows(ReglaNegocioException.class, () -> almacen.resolver(null));
    }

    @Test
    void unaRutaAbsolutaNoEscapaDeLaCarpeta() throws Exception {
        Path fuera = carpeta.getParent().resolve("secreto-fuera.mp3");
        Files.write(fuera, new byte[]{9});
        Path dentro = carpeta.resolve("secreto-fuera.mp3");
        Files.write(dentro, new byte[]{1});
        try {
            // de la ruta absoluta solo se usa el nombre; se sirve el de la carpeta
            assertEquals(dentro.toAbsolutePath().normalize(),
                    almacen.resolver(fuera.toString()));
        } finally {
            Files.deleteIfExists(fuera);
        }
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
