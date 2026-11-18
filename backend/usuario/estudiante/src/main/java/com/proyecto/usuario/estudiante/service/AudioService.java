package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.AudioRecibidoResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.AudioInvalidoException;
import com.proyecto.usuario.estudiante.exception.LecturaNoEncontradaException;
import com.proyecto.usuario.estudiante.repository.LecturaRepository;
import org.springframework.stereotype.Service;

// T-413 (US-13): recibe la grabación de la lectura y la valida
@Service
public class AudioService {

    public static final String VALIDADO = "validado";

    private final NivelService niveles;
    private final LecturaRepository lecturas;
    private final ValidadorWav validador;

    public AudioService(NivelService niveles, LecturaRepository lecturas, ValidadorWav validador) {
        this.niveles = niveles;
        this.lecturas = lecturas;
        this.validador = validador;
    }

    public AudioRecibidoResponse recibir(long estudianteId, int nivelId, long lecturaId, byte[] audio) {
        NivelDetalleResponse nivel = niveles.detalle(estudianteId, nivelId);   // 404 o 403 si no puede entrar
        if ("escritura".equals(nivel.tipoReto())) {
            throw new AudioInvalidoException("Este nivel es de escritura, no de lectura en voz alta");
        }
        lecturas.porId(lecturaId).orElseThrow(LecturaNoEncontradaException::new);
        long duracionMs = validador.validar(audio);
        return new AudioRecibidoResponse(null, nivelId, lecturaId, duracionMs, VALIDADO);
    }
}
