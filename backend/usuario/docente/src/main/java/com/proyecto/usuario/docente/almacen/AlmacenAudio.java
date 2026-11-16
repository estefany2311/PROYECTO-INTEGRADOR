package com.proyecto.usuario.docente.almacen;

import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

/**
 * T-372 (US-32): carpeta donde el módulo estudiante deja los audios de las
 * evidencias y utilidades para servirlos sin exponer rutas del servidor.
 *
 * <p>{@code audio_url} puede venir como nombre de archivo
 * ({@code evidencia-12.mp3}) o como la URL completa del CDN
 * ({@code https://cdn.../evidencia-12.mp3}); en ambos casos se usa solo el
 * nombre y se resuelve dentro de {@code app.evidencias.directorio}. Así el
 * docente nunca puede pedir una ruta arbitraria del servidor.</p>
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

    // solo letras, números, punto, guion y guion bajo; nada de rutas ni ".."
    private static final String NOMBRE_VALIDO = "[A-Za-z0-9][A-Za-z0-9._-]*";

    private final Path directorio;

    @Autowired
    public AlmacenAudio(@Value("${app.evidencias.directorio:./evidencias}") String directorio) {
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
     * Nombre del archivo a partir de un nombre simple o de una URL, sin la
     * query string ({@code ?v=1}) que a veces agregan los CDN.
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
     * Resuelve el archivo dentro de la carpeta configurada.
     *
     * @throws ReglaNegocioException 404 si el nombre no es válido o el archivo
     *                               no existe
     */
    public Path resolver(String audioUrl) {
        String nombre = nombreDe(audioUrl);
        if (!nombre.matches(NOMBRE_VALIDO)) {
            throw noDisponible();
        }
        Path archivo = directorio.resolve(nombre).normalize();
        if (!archivo.startsWith(directorio) || !Files.isRegularFile(archivo)) {
            throw noDisponible();
        }
        return archivo;
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
