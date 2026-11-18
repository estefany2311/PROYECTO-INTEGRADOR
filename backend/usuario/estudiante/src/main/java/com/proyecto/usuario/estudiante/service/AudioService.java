package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.AudioRecibidoResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.AudioInvalidoException;
import com.proyecto.usuario.estudiante.exception.LecturaNoEncontradaException;
import com.proyecto.usuario.estudiante.model.Lectura;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import com.proyecto.usuario.estudiante.repository.LecturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

// T-413/T-414 (US-13): recibe la grabación de la lectura, la guarda y crea la evidencia
@Service
public class AudioService {

    public static final String RECIBIDO = "recibido";

    private final NivelService niveles;
    private final LecturaRepository lecturas;
    private final ValidadorWav validador;
    private final AlmacenAudios almacen;
    private final EvidenciaRepository evidencias;
    private final Clock reloj;

    public AudioService(NivelService niveles, LecturaRepository lecturas, ValidadorWav validador,
                        AlmacenAudios almacen, EvidenciaRepository evidencias, Clock reloj) {
        this.niveles = niveles;
        this.lecturas = lecturas;
        this.validador = validador;
        this.almacen = almacen;
        this.evidencias = evidencias;
        this.reloj = reloj;
    }

    @Transactional
    public AudioRecibidoResponse recibir(long estudianteId, int nivelId, long lecturaId, byte[] audio) {
        NivelDetalleResponse nivel = niveles.detalle(estudianteId, nivelId);   // 404 o 403 si no puede entrar
        if ("escritura".equals(nivel.tipoReto())) {
            throw new AudioInvalidoException("Este nivel es de escritura, no de lectura en voz alta");
        }
        Lectura lectura = lecturas.porId(lecturaId).orElseThrow(LecturaNoEncontradaException::new);
        long duracionMs = validador.validar(audio);

        // T-414: primero el archivo; si falla la base se borra para no dejar audios sueltos
        String clave = almacen.guardar(estudianteId, nivelId, audio);
        try {
            LocalDateTime ahora = LocalDateTime.now(reloj);
            long evidenciaId = evidencias.crearAudio(estudianteId, nivelId, clave, ahora);
            evidencias.registrarLectura(evidenciaId, lecturaId, lectura.texto(), duracionMs, audio.length, ahora);
            return new AudioRecibidoResponse(evidenciaId, nivelId, lecturaId, duracionMs, RECIBIDO);
        } catch (RuntimeException e) {
            almacen.borrar(clave);
            throw e;
        }
    }
}
