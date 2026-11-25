package com.proyecto.usuario.estudiante.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

// WAV armados en memoria para las pruebas, como los que graba la tablet
public final class AudiosDePrueba {

    private AudiosDePrueba() {}

    public static byte[] wav(long duracionMs) {
        return wav(duracionMs, 1, 16_000, 16);
    }

    public static byte[] wav(long duracionMs, int canales, int frecuencia, int bits) {
        int bytesAudio = (int) (frecuencia * canales * (bits / 8) * duracionMs / 1000);
        ByteBuffer b = ByteBuffer.allocate(44 + bytesAudio).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + bytesAudio);
        b.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        b.put("fmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
        b.putShort((short) 1).putShort((short) canales).putInt(frecuencia)
                .putInt(frecuencia * canales * bits / 8).putShort((short) (canales * bits / 8)).putShort((short) bits);
        b.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(bytesAudio);
        return b.array();
    }
}
