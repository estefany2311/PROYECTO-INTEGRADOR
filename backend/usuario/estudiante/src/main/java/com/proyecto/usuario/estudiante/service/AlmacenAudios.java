package com.proyecto.usuario.estudiante.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// T-414: guarda los audios en una carpeta que también lee el backend docente (T-372)
@Component
public class AlmacenAudios {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path carpeta;
    private final Clock reloj;

    public AlmacenAudios(@Value("${estudiante.audios.carpeta}") String carpeta, Clock reloj) {
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
        this.reloj = reloj;
    }

    // Devuelve la ruta relativa que se guarda en evidencia.audio_url, por ejemplo est_7/nivel_3_20261117-101530_ab12cd34.wav
    public String guardar(long estudianteId, int nivelId, byte[] audio) {
        String nombre = "nivel_%d_%s_%s.wav".formatted(
                nivelId, LocalDateTime.now(reloj).format(FECHA), UUID.randomUUID().toString().substring(0, 8));
        String clave = "est_" + estudianteId + "/" + nombre;
        Path destino = ruta(clave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, audio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el audio", e);
        }
        return clave;
    }

    public Path ruta(String clave) {
        Path ruta = carpeta.resolve(clave).normalize();
        if (!ruta.startsWith(carpeta)) throw new IllegalArgumentException("Ruta de audio inválida");
        return ruta;
    }

    public void borrar(String clave) {
        try {
            Files.deleteIfExists(ruta(clave));
        } catch (IOException ignorado) {
            // si no se puede borrar, la limpieza no debe ocultar el error original
        }
    }
}
