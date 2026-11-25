package com.proyecto.usuario.docente.almacen;

import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

/**
 * T-372 (US-32): carpeta donde el módulo estudiante deja los audios de las
 * evidencias y utilidades para servirlos sin exponer rutas del servidor.
 *
 * <p>{@code audio_url} se guarda como ruta relativa a esa carpeta, con la
 * subcarpeta del alumno delante ({@code est_7/nivel_1_20261117-221100_ab12cd34.wav}).
 * También se acepta una URL completa ({@code https://cdn.../audio.mp3}); en ese
 * caso se usa solo el nombre del archivo. En ambos casos la ruta se resuelve
 * dentro de la carpeta y se rechaza cualquier intento de salir de ella.</p>
 */
@Component
public class AlmacenAudio {

    private static final Map<String, String> TIPOS = Map.of(
            "mp3", "audio/mpeg",
            "wav", "audio/wav",
            "m4a", "audio/mp4",
            "aac", "audio/aac",
            "ogg", "audio/ogg",
            "webm", "audio/webm");

    // solo letras, números, punto, guion y guion bajo; nada de ".." ni ":" de unidad
    private static final String NOMBRE_VALIDO = "[A-Za-z0-9][A-Za-z0-9._-]*";

    private final Path directorio;

    @Autowired
    public AlmacenAudio(@Value("${app.evidencias.directorio:${user.home}/plataforma-educativa/audios}")
                        String directorio) {
        this(Paths.get(directorio));
    }

    // constructor para las pruebas: no depende de Spring
    AlmacenAudio(Path directorio) {
        this.directorio = directorio.toAbsolutePath().normalize();
    }

    public Path directorio() {
        return directorio;
    }

    /**
     * Nombre del archivo a partir de una URL o ruta, sin la query string
     * ({@code ?v=1}) que a veces agregan los CDN. Se usa para las URLs
     * completas; las rutas relativas conservan sus subcarpetas en
     * {@link #resolver(String)}.
     */
    public static String nombreDe(String audioUrl) {
        String nombre = audioUrl == null ? "" : audioUrl.trim();
        int corte = Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\'));
        if (corte >= 0) {
            nombre = nombre.substring(corte + 1);
        }
        int query = nombre.indexOf('?');
        if (query >= 0) {
            nombre = nombre.substring(0, query);
        }
        return nombre;
    }

    /**
     * Resuelve el archivo dentro de la carpeta configurada. Acepta una ruta
     * relativa con subcarpetas ({@code est_7/audio.wav}) o una URL completa
     * (de la que se usa solo el nombre).
     *
     * @throws ReglaNegocioException 404 si la ruta no es válida, intenta salir
     *                               de la carpeta o el archivo no existe
     */
    public Path resolver(String audioUrl) {
        Path relativa = rutaDe(audioUrl);
        Path archivo = directorio.resolve(relativa).normalize();
        if (!archivo.startsWith(directorio) || !Files.isRegularFile(archivo)) {
            throw noDisponible();
        }
        return archivo;
    }

    /**
     * Convierte {@code audio_url} en una ruta relativa segura. Las URLs
     * completas se reducen a su nombre; las rutas relativas conservan sus
     * subcarpetas y cada segmento debe ser un nombre simple.
     */
    private Path rutaDe(String audioUrl) {
        String valor = audioUrl == null ? "" : audioUrl.trim();
        int query = valor.indexOf('?');
        if (query >= 0) {
            valor = valor.substring(0, query);
        }
        String esquema = valor.toLowerCase(Locale.ROOT);
        if (esquema.startsWith("http://") || esquema.startsWith("https://")) {
            valor = nombreDe(valor);
        }

        String ruta = valor.replace('\\', '/');
        // split con -1 conserva los segmentos vacíos: "/x", "//servidor" y "a//b" se rechazan
        for (String segmento : ruta.split("/", -1)) {
            if (!segmento.matches(NOMBRE_VALIDO)) {
                throw noDisponible();
            }
        }
        try {
            Path relativa = Paths.get(ruta).normalize();
            if (relativa.isAbsolute() || relativa.getNameCount() == 0) {
                throw noDisponible();
            }
            return relativa;
        } catch (InvalidPathException e) {
            throw noDisponible();
        }
    }

    /** Tipo MIME según la extensión; los desconocidos se sirven como binario. */
    public String tipoContenido(Path archivo) {
        String nombre = archivo.getFileName().toString().toLowerCase(Locale.ROOT);
        int punto = nombre.lastIndexOf('.');
        String extension = punto >= 0 ? nombre.substring(punto + 1) : "";
        return TIPOS.getOrDefault(extension, "application/octet-stream");
    }

    private ReglaNegocioException noDisponible() {
        return new ReglaNegocioException("El audio no está disponible", HttpStatus.NOT_FOUND);
    }
}
