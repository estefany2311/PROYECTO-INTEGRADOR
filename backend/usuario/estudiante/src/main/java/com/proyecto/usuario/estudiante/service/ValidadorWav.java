package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.exception.AudioInvalidoException;
import com.proyecto.usuario.estudiante.exception.FormatoAudioNoSoportadoException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

// T-413: revisa la cabecera del WAV y calcula la duración (no se confía en lo que diga la tablet)
@Component
public class ValidadorWav {

    public static final int FRECUENCIA_HZ = 16_000;
    public static final int BYTES_POR_SEGUNDO = FRECUENCIA_HZ * 2;   // mono, 16 bits
    public static final long DURACION_MINIMA_MS = 1_000;
    public static final long DURACION_MAXIMA_MS = 125_000;            // 2 minutos de la app + margen

    // Devuelve la duración en milisegundos
    public long validar(byte[] audio) {
        if (audio == null || audio.length == 0) {
            throw new AudioInvalidoException("No llegó ninguna grabación");
        }
        if (audio.length < 44 || !texto(audio, 0).equals("RIFF") || !texto(audio, 8).equals("WAVE")) {
            throw new FormatoAudioNoSoportadoException("no es un archivo WAV");
        }
        ByteBuffer datos = ByteBuffer.wrap(audio).order(ByteOrder.LITTLE_ENDIAN);
        boolean formatoRevisado = false;
        int posicion = 12;
        while (posicion + 8 <= audio.length) {
            String nombre = texto(audio, posicion);
            int tamano = datos.getInt(posicion + 4);
            int inicio = posicion + 8;
            if (!nombre.equals("data") && (tamano < 0 || tamano > audio.length - inicio)) break;
            if (nombre.equals("fmt ")) {
                if (tamano < 16) throw new FormatoAudioNoSoportadoException("formato incompleto");
                revisarFormato(datos, inicio);
                formatoRevisado = true;
            } else if (nombre.equals("data")) {
                if (!formatoRevisado) throw new FormatoAudioNoSoportadoException("falta el formato");
                // Si la cabecera quedó mal (por ejemplo en 0), se usa lo que realmente llegó
                int bytesAudio = audio.length - inicio;
                if (tamano > 0 && tamano < bytesAudio) bytesAudio = tamano;
                return revisarDuracion(bytesAudio * 1000L / BYTES_POR_SEGUNDO);
            }
            posicion = inicio + tamano + (tamano % 2);
        }
        throw new FormatoAudioNoSoportadoException("no tiene audio");
    }

    private void revisarFormato(ByteBuffer datos, int inicio) {
        if (datos.getShort(inicio) != 1) throw new FormatoAudioNoSoportadoException("no es PCM");
        if (datos.getShort(inicio + 2) != 1) throw new FormatoAudioNoSoportadoException("no es mono");
        if (datos.getInt(inicio + 4) != FRECUENCIA_HZ) throw new FormatoAudioNoSoportadoException("no es de 16 kHz");
        if (datos.getShort(inicio + 14) != 16) throw new FormatoAudioNoSoportadoException("no es de 16 bits");
    }

    private long revisarDuracion(long duracionMs) {
        if (duracionMs < DURACION_MINIMA_MS) {
            throw new AudioInvalidoException("La grabación es muy corta. Intenta leer otra vez");
        }
        if (duracionMs > DURACION_MAXIMA_MS) {
            throw new AudioInvalidoException("La grabación pasa de 2 minutos");
        }
        return duracionMs;
    }

    private static String texto(byte[] audio, int desde) {
        return new String(audio, desde, 4, StandardCharsets.US_ASCII);
    }
}
